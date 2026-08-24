package org.jmouse.files.jpa.directory;

import jakarta.persistence.EntityManager;
import org.jmouse.files.OwnerReference;
import org.jmouse.files.directory.DirectoryPath;
import org.jmouse.files.directory.DirectorySlugs;
import org.jmouse.files.exception.DirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 🌳 The tree: resolving a root, making a folder, moving one, reading a subtree.
 *
 * <h3>⚠️ Every write renumbers, and the numbering is the point</h3>
 *
 * <p>A nested set makes "everything under here" a single indexed range, which is what an
 * authorization engine asks on every read of a subtree. The cost is paid on writes instead: inserting
 * a directory widens every range to its right, and moving one shifts a whole block. Both are done in
 * bulk statements rather than by walking entities, because a tree with a few thousand folders would
 * otherwise load most of itself into memory to add one.</p>
 *
 * <p>⚠️ <strong>Bulk statements bypass the persistence context.</strong> Anything already loaded still
 * carries the old numbering afterwards, so each write clears the context before returning. Skipping
 * that is how a caller ends up reading numbers that were true a moment ago and writing them back.</p>
 *
 * <p>Transaction demarcation is the caller's, and a renumbering is only safe inside one.</p>
 */
public class StorageDirectories {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageDirectories.class);

    private final EntityManager    entityManager;
    private final Supplier<String> identifiers;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     * @param identifiers   where a new directory's identifier comes from
     */
    public StorageDirectories(EntityManager entityManager, Supplier<String> identifiers) {
        this.entityManager = entityManager;
        this.identifiers   = identifiers;
    }

    // ── Reading ───────────────────────────────────────────────────────────────

    /**
     * 🔎 The directory at a path, if it is there.
     *
     * @param path the path, e.g. {@code innoventa/files/manuals}
     * @return the directory, or empty
     */
    public Optional<StorageDirectory> find(String ownerKey, DirectoryPath path) {
        return entityManager.createQuery(
                "SELECT directory FROM StorageDirectory directory "
                + "WHERE directory.ownerKey = :ownerKey AND directory.path = :path",
                StorageDirectory.class)
            .setParameter("ownerKey", ownerKey)
            .setParameter("path", path.toString())
            .getResultStream()
            .findFirst();
    }

    /**
     * 🔎 One directory by identifier, or a refusal naming it.
     *
     * @param identifier the directory
     * @return the directory
     */
    public StorageDirectory require(String identifier) {
        StorageDirectory directory = entityManager.find(StorageDirectory.class, identifier);

        if (directory == null) {
            throw new DirectoryException("No such directory: %s".formatted(identifier));
        }

        return directory;
    }

    /**
     * 🌿 Everything under a directory, itself included, in tree order.
     *
     * <p>One indexed range query — which is the whole reason the numbering exists.</p>
     *
     * @param directory the directory
     * @return the subtree, shallowest first
     */
    public List<StorageDirectory> subtreeOf(StorageDirectory directory) {
        return entityManager.createQuery(
                "SELECT directory FROM StorageDirectory directory "
                + "WHERE directory.ownerKey = :ownerKey "
                + "AND directory.treeLeft >= :left AND directory.treeRight <= :right "
                + "ORDER BY directory.treeLeft", StorageDirectory.class)
            .setParameter("ownerKey", directory.getOwnerKey())
            .setParameter("left", directory.getTreeLeft())
            .setParameter("right", directory.getTreeRight())
            .getResultList();
    }

    /**
     * 🌱 Every root, which is every place a product declared.
     *
     * @return the roots, by path
     */
    public List<StorageDirectory> roots(String ownerKey) {
        return entityManager.createQuery(
                "SELECT directory FROM StorageDirectory directory "
                + "WHERE directory.root = true AND directory.ownerKey = :ownerKey "
                + "ORDER BY directory.path", StorageDirectory.class)
            .setParameter("ownerKey", ownerKey)
            .getResultList();
    }

    /**
     * 🌿 Every directory one sits inside, itself excluded.
     *
     * <p>One indexed range query — asked on every authorized read of a file, so it cannot be a walk up
     * the parent pointers.</p>
     *
     * @param identifier the directory
     * @return its ancestors, outermost first
     */
    public List<StorageDirectory> ancestorsOf(String identifier) {
        StorageDirectory directory = entityManager.find(StorageDirectory.class, identifier);

        if (directory == null) {
            return List.of();
        }

        return entityManager.createQuery(
                "SELECT directory FROM StorageDirectory directory "
                + "WHERE directory.ownerKey = :ownerKey "
                + "AND directory.treeLeft < :left AND directory.treeRight > :right "
                + "ORDER BY directory.treeLeft", StorageDirectory.class)
            .setParameter("ownerKey", directory.getOwnerKey())
            .setParameter("left", directory.getTreeLeft())
            .setParameter("right", directory.getTreeRight())
            .getResultList();
    }

    /**
     * 🌿 Everything under any of these, themselves included.
     *
     * <p>⚠️ One query for the lot rather than one per directory: a caller holding grants at a dozen
     * places asks this on every read, and a dozen round trips per request is how an authorization model
     * becomes the reason a product feels slow.</p>
     *
     * @param identifiers the directories
     * @return the union of their subtrees
     */
    public List<StorageDirectory> subtreesOf(Collection<String> identifiers) {
        List<StorageDirectory> roots = entityManager.createQuery(
                "SELECT directory FROM StorageDirectory directory WHERE directory.id IN :identifiers",
                StorageDirectory.class)
            .setParameter("identifiers", identifiers)
            .getResultList();

        if (roots.isEmpty()) {
            return List.of();
        }

        List<StorageDirectory> found = new ArrayList<>();

        for (StorageDirectory each : roots) {
            found.addAll(subtreeOf(each));
        }

        return found;
    }

    // ── Writing ───────────────────────────────────────────────────────────────

    /**
     * 🌱 The root at this path, made if it is not there yet.
     *
     * <p>⚠️ <strong>Idempotent, because roots are declared by code rather than by people.</strong>
     * {@code innoventa/files} is named in a product's own configuration and has to resolve to the same
     * directory on every start, including the first — an identifier cannot be written down anywhere
     * because it differs per installation.</p>
     *
     * @param path a root path, {@code <application>/<purpose>}
     * @return the root
     */
    public StorageDirectory requireRoot(String path) {
        return requireRoot(StorageDirectory.INSTALLATION, path);
    }

    /**
     * 🌱 The root at this path in one owner's tree, made if it is not there yet.
     *
     * <p>⚠️ <strong>The owner is what lets a product with a personal file cabinet use this tree at
     * all.</strong> Kiwi and Tessera pass {@link StorageDirectory#INSTALLATION} — their files belong to
     * sections and issues. Innoventa passes an account, because every user genuinely has their own
     * folders, and one shared tree would be a different product.</p>
     *
     * @param owner whose tree
     * @param path  a root path, {@code <application>/<purpose>}
     * @return the root
     */
    public StorageDirectory requireRoot(OwnerReference owner, String path) {
        return requireRoot(owner.toString(), path);
    }

    /**
     * 🌱 The root at this path in one owner's tree, made if it is not there yet.
     *
     * <p>⚠️ Prefer the {@link OwnerReference} overload above: a bare identifier has no kind in it, and a
     * product with two sorts of owner cannot tell its trees apart afterwards.</p>
     *
     * @param ownerKey whose tree, as {@code KIND:id}, or {@link StorageDirectory#INSTALLATION}
     * @param path     a root path, {@code <application>/<purpose>}
     * @return the root
     */
    public StorageDirectory requireRoot(String ownerKey, String path) {
        DirectoryPath rootPath = DirectoryPath.ofRoot(path);

        return find(ownerKey, rootPath).orElseGet(() -> createRoot(ownerKey, rootPath));
    }

    /**
     * 🧭 The directory at a whole path, with every folder on the way to it made if it is not there yet.
     *
     * <p>{@code tessera/attachments/issues/TSSR/TSSR-42} in one call, from nothing.</p>
     *
     * <h3>⚠️ Why this is here rather than in each product</h3>
     *
     * <p>Anything that files by a <em>computed</em> path — one folder per issue, per month, per
     * account — needs the same walk: look for the next segment, make it when it is missing, descend.
     * Written product-side it is the same twenty lines three times, and each copy has to know two
     * things about this class that are not obvious from outside it: that {@link #create} slugs the
     * name it is given, so a lookup must be by the <em>slug</em>; and that {@code create} makes a
     * <em>second</em> folder with a distinguishing suffix when the slug is taken, so a walk that
     * creates without looking first ends up with {@code tssr-42} and {@code tssr-42-2} side by side.
     * Neither is discoverable by writing the obvious loop and running it once.</p>
     *
     * <h3>⚠️ Idempotent, and meant to be called every time</h3>
     *
     * <p>Existing folders are read rather than remade, so this belongs on the upload path itself. There
     * is nothing to cache and nothing to seed: asking for a path is how a path comes to exist.</p>
     *
     * <h3>⚠️ The race is the CALLER's to absorb, and that is not an omission</h3>
     *
     * <p>Two requests filing into the same new folder at the same moment both read nothing and both
     * insert; the loser gets a unique-constraint violation on {@code (owner_key, path)}, which means
     * precisely that the folder it wanted now exists. Re-reading is the right answer, and this method
     * cannot be the thing that does it — a failed flush leaves the persistence context and the
     * transaction unusable, so a retry has to happen in a <em>new</em> transaction, and this library
     * demarcates none by design.</p>
     *
     * <p>So a caller that can race wraps the call the way {@code FileDirectories} in Innoventa already
     * wraps {@link #requireRoot}: {@code REQUIRES_NEW}, and one retry on the integrity violation. A
     * caller that cannot — a bootstrap step, a single-threaded import — needs neither.</p>
     *
     * @param ownerKey whose tree, as {@code KIND:id}, or {@link StorageDirectory#INSTALLATION}
     * @param path     the whole path, root first — {@code <application>/<purpose>/…}
     * @return the directory at that path
     */
    public StorageDirectory requirePath(String ownerKey, DirectoryPath path) {
        List<String> segments = path.segments();

        // ⚠️ Refused before anything is written, never half-made. A path too deep discovered three
        // folders in would leave those three behind, and the caller would retry into a tree that had
        // silently grown branches nobody asked for.
        if (segments.size() > StorageDirectory.MAXIMUM_DEPTH) {
            throw new DirectoryException(
                "'%s' is %d deep, and %d is as far as this tree goes."
                    .formatted(path, segments.size(), StorageDirectory.MAXIMUM_DEPTH));
        }

        StorageDirectory directory = requireRoot(ownerKey, path.root().toString());

        for (String segment : segments.subList(DirectoryPath.ROOT_DEPTH, segments.size())) {
            directory = requireChild(directory, segment);
        }

        return directory;
    }

    /**
     * 🧭 The directory at a whole written path, made if it is not there yet.
     *
     * @param ownerKey whose tree, as {@code KIND:id}, or {@link StorageDirectory#INSTALLATION}
     * @param path     the whole path as written, e.g. {@code tessera/attachments/issues/TSSR}
     * @return the directory at that path
     */
    public StorageDirectory requirePath(String ownerKey, String path) {
        return requirePath(ownerKey, DirectoryPath.of(path));
    }

    /**
     * 🧭 The directory at a whole path in one owner's tree, made if it is not there yet.
     *
     * @param owner whose tree
     * @param path  the whole path, root first
     * @return the directory at that path
     */
    public StorageDirectory requirePath(OwnerReference owner, DirectoryPath path) {
        return requirePath(owner.toString(), path);
    }

    /**
     * 📁 One named folder inside another, read when it is already there.
     *
     * <p>⚠️ <strong>Looked up by the SLUG, not by the name.</strong> {@code create} slugs what it is
     * given, so {@code "TSSR-42"} becomes {@code tssr-42} in the path — and a lookup by the raw name
     * would miss it, create a second folder, and be given {@code tssr-42-2} because the first one has
     * the slug. Two folders, one issue, and no error anywhere.</p>
     */
    private StorageDirectory requireChild(StorageDirectory parent, String name) {
        DirectoryPath childPath = DirectoryPath.of(parent.getPath()).resolve(DirectorySlugs.of(name));

        return find(parent.getOwnerKey(), childPath).orElseGet(() -> create(parent, name));
    }

    /**
     * 📁 A folder inside another.
     *
     * @param parent where it goes
     * @param name   what a person calls it
     * @return the new directory
     */
    public StorageDirectory create(StorageDirectory parent, String name) {
        if (parent.getDepth() + 1 > StorageDirectory.MAXIMUM_DEPTH) {
            throw new DirectoryException(
                "'%s' is already %d deep, and %d is as far as this tree goes."
                    .formatted(parent.getPath(), parent.getDepth(), StorageDirectory.MAXIMUM_DEPTH));
        }

        String slug = uniqueSlug(parent, name);
        int    left = parent.getTreeRight();

        // Make room: everything at or right of the parent's right bound shifts two places.
        shift(parent.getOwnerKey(), "treeLeft", left, 2);
        shift(parent.getOwnerKey(), "treeRight", left, 2);

        StorageDirectory directory = new StorageDirectory(
            identifiers.get(), parent.getOwnerKey(), parent.getId(), name.trim(), slug,
            DirectoryPath.of(parent.getPath()).resolve(slug).toString(), false,
            left, left + 1, parent.getDepth() + 1, nextSortOrder(parent.getId()));

        entityManager.persist(directory);
        entityManager.flush();
        entityManager.clear();

        return require(directory.getId());
    }

    /**
     * 🏷️ Rename a folder.
     *
     * <p>⚠️ The slug and therefore the path change with it, for the whole subtree — which is why a
     * <strong>root</strong> may not be renamed: its path is the storage namespace of every object
     * filed beneath it, and those keys are already written.</p>
     *
     * @param directory the directory
     * @param name      the new name
     * @return the renamed directory
     */
    public StorageDirectory rename(StorageDirectory directory, String name) {
        refuseRootChange(directory, "renamed");

        StorageDirectory parent = require(directory.getParentId());
        String           slug   = uniqueSlug(parent, name, directory.getId());
        String           path   = DirectoryPath.of(parent.getPath()).resolve(slug).toString();

        rewritePaths(directory, path);

        directory.setName(name.trim());
        directory.setSlug(slug);
        directory.setPath(path);

        entityManager.flush();
        entityManager.clear();

        return require(directory.getId());
    }

    /**
     * 📦 Move a folder under another.
     *
     * <p>⚠️ Refused when the destination is inside the folder being moved — a tree cannot contain
     * itself, and the numbering would be unrecoverable rather than merely wrong.</p>
     *
     * @param directory   the directory to move
     * @param destination where it should go
     * @return the moved directory
     */
    public StorageDirectory moveTo(StorageDirectory directory, StorageDirectory destination) {
        refuseRootChange(directory, "moved");

        if (directory.equals(destination) || directory.contains(destination)) {
            throw new DirectoryException(
                "'%s' cannot be moved into itself or into one of its own folders."
                    .formatted(directory.getPath()));
        }

        // ⚠️ Never across owners. The trees are independent by construction — separate numbering,
        // separate paths — so a move between them would corrupt both, and it would also hand one
        // person's folder to another, which no route above this is checking for because no route above
        // this can see that it happened.
        if (!directory.getOwnerKey().equals(destination.getOwnerKey())) {
            throw new DirectoryException(
                "'%s' and '%s' are in different trees and nothing moves between them."
                    .formatted(directory.getPath(), destination.getPath()));
        }

        int span    = directory.getTreeRight() - directory.getTreeLeft() + 1;
        int newDepth = destination.getDepth() + 1;
        int deepest  = newDepth + subtreeHeight(directory);

        if (deepest > StorageDirectory.MAXIMUM_DEPTH) {
            throw new DirectoryException(
                "Moving '%s' there would put a folder %d deep, and %d is as far as this tree goes."
                    .formatted(directory.getPath(), deepest, StorageDirectory.MAXIMUM_DEPTH));
        }

        // ⚠️ The path has to be rewritten too, and BEFORE the renumbering — `subtreeOf` reads the
        // current numbering to find what moves, so it must still be the old one. Leaving this out was
        // the bug this method shipped with: `renumber()` only touches treeLeft, treeRight and depth, so
        // a moved folder kept the path of where it used to be — and `path` is both the unique key and
        // what `find(DirectoryPath)` looks up, so the tree would resolve to the wrong place silently.
        String slug    = uniqueSlug(destination, directory.getName(), directory.getId());
        String newPath = DirectoryPath.of(destination.getPath()).resolve(slug).toString();

        rewritePaths(directory, newPath);

        directory.setSlug(slug);
        directory.setPath(newPath);

        // Expressed as a new parent and executed as a renumbering: the pointer is the authority, so
        // the numbering is rebuilt from it rather than patched in place.
        directory.setParentId(destination.getId());
        entityManager.flush();

        renumber(directory.getOwnerKey());
        entityManager.clear();

        LOGGER.debug("📦 Moved directory {} under {} ({} node(s))",
                     directory.getId(), destination.getPath(), span);

        return require(directory.getId());
    }

    /**
     * 🗑️ Remove a folder.
     *
     * <p>⚠️ Refused while anything is still inside it, unless the caller says otherwise. Deleting a
     * folder is one click and losing a subtree is not recoverable, so the default is the cautious one
     * and a product that wants the other behaviour has to ask for it.</p>
     *
     * @param directory   the directory
     * @param withSubtree whether folders inside it go too
     */
    public void delete(StorageDirectory directory, boolean withSubtree) {
        if (directory.isRoot()) {
            throw new DirectoryException(
                "'%s' is a root — it is named by configuration and is not deleted through the tree."
                    .formatted(directory.getPath()));
        }

        List<StorageDirectory> subtree = subtreeOf(directory);

        if (subtree.size() > 1 && !withSubtree) {
            throw new DirectoryException(
                "'%s' still holds %d folder(s). Empty it first, or ask for it to go with its contents."
                    .formatted(directory.getPath(), subtree.size() - 1));
        }

        for (StorageDirectory removed : subtree) {
            entityManager.remove(removed);
        }

        entityManager.flush();
        renumber(directory.getOwnerKey());
        entityManager.clear();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private StorageDirectory createRoot(String ownerKey, DirectoryPath path) {
        int highest = highestBound(ownerKey);

        StorageDirectory root = new StorageDirectory(
            identifiers.get(), ownerKey, null, path.toString(),
            DirectorySlugs.of(path.segments().getLast()),
            path.toString(), true, highest + 1, highest + 2, 1, roots(ownerKey).size());

        entityManager.persist(root);
        entityManager.flush();
        entityManager.clear();

        LOGGER.info("🌱 Created storage root '{}' for owner '{}'", path, ownerKey);

        return require(root.getId());
    }

    private void refuseRootChange(StorageDirectory directory, String what) {
        if (directory.isRoot()) {
            throw new DirectoryException(
                ("'%s' is a root and cannot be %s: its path is the storage namespace of every object "
                 + "filed beneath it, and those keys are already written.")
                    .formatted(directory.getPath(), what));
        }
    }

    private String uniqueSlug(StorageDirectory parent, String name) {
        return uniqueSlug(parent, name, null);
    }

    private String uniqueSlug(StorageDirectory parent, String name, String exceptId) {
        String candidate = DirectorySlugs.of(name);
        String path      = DirectoryPath.of(parent.getPath()).resolve(candidate).toString();

        if (!pathTaken(parent.getOwnerKey(), path, exceptId)) {
            return candidate;
        }

        for (int attempt = 2; attempt < 1000; attempt++) {
            String distinct = DirectorySlugs.of(name, attempt);

            if (!pathTaken(parent.getOwnerKey(),
                           DirectoryPath.of(parent.getPath()).resolve(distinct).toString(), exceptId)) {
                return distinct;
            }
        }

        throw new DirectoryException(
            "'%s' already exists in '%s', and so do a great many variations of it."
                .formatted(name, parent.getPath()));
    }

    /**
     * ❓ Whether a path already belongs to some other directory.
     *
     * <p>⚠️ Two queries rather than one with {@code (:exceptId IS NULL OR …)}. A JPQL parameter compared
     * against {@code NULL} is the kind of expression providers disagree about, and this runs against both
     * MySQL and PostgreSQL through Hibernate — a portability wart is not worth saving four lines.</p>
     *
     * @param path     the path to test
     * @param exceptId a directory allowed to hold it, or {@code null}
     * @return {@code true} when somebody else has it
     */
    private boolean pathTaken(String ownerKey, String path, String exceptId) {
        if (exceptId == null) {
            return entityManager.createQuery(
                    "SELECT COUNT(directory) FROM StorageDirectory directory "
                    + "WHERE directory.ownerKey = :ownerKey AND directory.path = :path", Long.class)
                .setParameter("ownerKey", ownerKey)
                .setParameter("path", path)
                .getSingleResult() > 0;
        }

        return entityManager.createQuery(
                "SELECT COUNT(directory) FROM StorageDirectory directory "
                + "WHERE directory.ownerKey = :ownerKey AND directory.path = :path "
                + "AND directory.id <> :exceptId", Long.class)
            .setParameter("ownerKey", ownerKey)
            .setParameter("path", path)
            .setParameter("exceptId", exceptId)
            .getSingleResult() > 0;
    }

    private void rewritePaths(StorageDirectory directory, String newPath) {
        String oldPath = directory.getPath();

        for (StorageDirectory descendant : subtreeOf(directory)) {
            if (!descendant.equals(directory)) {
                descendant.setPath(newPath + descendant.getPath().substring(oldPath.length()));
            }
        }
    }

    private int subtreeHeight(StorageDirectory directory) {
        return subtreeOf(directory).stream()
                .mapToInt(StorageDirectory::getDepth)
                .max()
                .orElse(directory.getDepth()) - directory.getDepth();
    }

    /**
     * 🔢 Widen the numbering to the right of an insertion point — within ONE owner's tree.
     *
     * <p>⚠️ Scoped by owner, and it has to be: without that, one person adding a folder renumbers every
     * other person's tree in the installation. Correctness would survive it, since the ranges stay
     * contiguous either way, but a write that touches every row in the table is not a write anybody
     * expects from making a folder.</p>
     */
    private void shift(String ownerKey, String bound, int from, int by) {
        entityManager.createQuery(
                ("UPDATE StorageDirectory directory SET directory.%s = directory.%s + :by "
                 + "WHERE directory.ownerKey = :ownerKey AND directory.%s >= :from")
                    .formatted(bound, bound, bound))
            .setParameter("ownerKey", ownerKey)
            .setParameter("by", by)
            .setParameter("from", from)
            .executeUpdate();
    }

    private int highestBound(String ownerKey) {
        Integer highest = entityManager.createQuery(
                "SELECT MAX(directory.treeRight) FROM StorageDirectory directory "
                + "WHERE directory.ownerKey = :ownerKey", Integer.class)
            .setParameter("ownerKey", ownerKey)
            .getSingleResult();

        return highest == null ? 0 : highest;
    }

    private int nextSortOrder(String parentId) {
        Integer highest = entityManager.createQuery(
                "SELECT MAX(directory.sortOrder) FROM StorageDirectory directory "
                + "WHERE directory.parentId = :parentId", Integer.class)
            .setParameter("parentId", parentId)
            .getSingleResult();

        return highest == null ? 0 : highest + 1;
    }

    /**
     * 🔢 Rebuild the whole numbering from the parent pointers.
     *
     * <p>⚠️ Scoped to ONE owner's tree: rebuilding the whole table because somebody moved their own
     * folder would be a write nobody expects, and the trees are independent by construction.</p>
     *
     * <p>⚠️ <strong>Rebuilt rather than patched, and that is the safety property.</strong> Patching a
     * move means four range updates that must all be right; getting one wrong leaves a numbering that
     * looks plausible and answers subtree questions incorrectly, which is a silent authorization bug.
     * Rebuilding from the pointers cannot drift, and the pointers are what a person edits.</p>
     */
    private void renumber(String ownerKey) {
        List<StorageDirectory> all = entityManager.createQuery(
                "SELECT directory FROM StorageDirectory directory "
                + "WHERE directory.ownerKey = :ownerKey "
                + "ORDER BY directory.sortOrder, directory.name", StorageDirectory.class)
            .setParameter("ownerKey", ownerKey)
            .getResultList();

        int counter = 0;

        for (StorageDirectory root : all.stream().filter(StorageDirectory::isRoot).toList()) {
            counter = number(root, all, counter + 1, 1);
        }

        entityManager.flush();
    }

    private int number(StorageDirectory directory, List<StorageDirectory> all, int left, int depth) {
        int counter = left;

        directory.setTreeLeft(left);
        directory.setDepth(depth);

        for (StorageDirectory child : all.stream()
                .filter(candidate -> directory.getId().equals(candidate.getParentId()))
                .toList()) {
            counter = number(child, all, counter + 1, depth + 1);
        }

        directory.setTreeRight(counter + 1);

        return counter + 1;
    }
}
