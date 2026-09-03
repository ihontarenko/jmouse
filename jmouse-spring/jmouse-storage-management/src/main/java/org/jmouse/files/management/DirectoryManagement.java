package org.jmouse.files.management;

import org.jmouse.files.directory.DirectoryConfigurationKind;
import org.jmouse.files.directory.DirectoryConfigurationKinds;
import org.jmouse.files.jpa.directory.StorageDirectories;
import org.jmouse.files.jpa.directory.StorageDirectory;
import org.jmouse.files.jpa.directory.StorageDirectoryConfigurations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🌳 The whole of what a directory endpoint does, minus the routing and minus the authorization.
 *
 * <p>The tree's counterpart to {@link FileManagement}, and it exists for exactly the reason that one
 * gives for itself: <em>somebody has to own the transaction, and when the controller is mounted the only
 * caller is a controller.</em></p>
 *
 * <h3>⚠️ Why this class had to exist at all</h3>
 *
 * <p>{@link StorageDirectories} is a nested set, so every write renumbers — inserting a folder widens
 * every range to its right and moving one shifts a whole block. Both are done in bulk statements rather
 * than by walking entities, and a bulk statement without an active transaction does not fail quietly: it
 * throws {@code TransactionRequiredException}. Its own Javadoc says so and leaves demarcation to the
 * caller, which is right for a product calling from its own service and is nothing at all for a product
 * that merely mounts {@link DirectoryController}.</p>
 *
 * <p>Before this class, {@code POST /directories} answered <strong>500</strong> for every caller of the
 * mounted controller, and so did every move and every delete — the whole write half of the tree API.
 * {@link FileManagement} had the boundary from the beginning, which is why the file half worked and only
 * the tree did not.</p>
 *
 * <h3>⚠️ Not on the controller, and not on StorageDirectories</h3>
 *
 * <p>Not on the controller, because a transactional web layer holds a connection open across argument
 * binding, the handler and whatever an interceptor does around it — and {@code FileManagement} already
 * ruled that "a transactional controller is worse than this".</p>
 *
 * <p>Not on {@link StorageDirectories}, because its contract is deliberately annotation-free: a product
 * driving the tree from inside its own transaction must be able to, and an inner {@code REQUIRED} that
 * silently joins is fine while an annotation on a class also wraps every read.</p>
 *
 * <h3>⚠️ Nothing here asks who is calling</h3>
 *
 * <p>The same rule as its neighbour. Routes are gated before a handler runs, by one engine, against one
 * policy; a second opinion at this level is a rule somebody edits in one place. In particular the
 * destination of a {@link #move} is <strong>not</strong> checked here — it sits in the request body,
 * where no external rule can see it, and the product wiring this module up is told to check it. See
 * {@link DirectoryController#move}.</p>
 */
public class DirectoryManagement {

    private final StorageDirectories             directories;
    private final StorageDirectoryConfigurations configurations;
    private final DirectoryConfigurationKinds    kinds;

    /**
     * One per kind of configuration — they resolve what applies to a folder, and they are what has to
     * be told when a branch stops meaning what it meant.
     */
    private final List<DirectoryConfigurationResolver> resolvers;

    /** Where "this folder changed its mind" is announced, or {@code null} where nobody is listening. */
    private final ApplicationEventPublisher      events;

    /**
     * 🏗️ Own the tree's transactions.
     *
     * @param directories the tree
     */
    public DirectoryManagement(StorageDirectories directories) {
        this(directories, null, null, List.of(), null);
    }

    /**
     * 🏗️ Own the tree's transactions, and what its folders say about themselves.
     *
     * @param directories    the tree
     * @param configurations what folders say about themselves, or {@code null} where nothing may
     * @param kinds          what kinds this installation knows about
     * @param resolvers      one per kind — what applies to a folder, and what to forget when it changes
     * @param events         where a change is announced, or {@code null} to announce nothing
     */
    public DirectoryManagement(StorageDirectories directories,
                               StorageDirectoryConfigurations configurations,
                               DirectoryConfigurationKinds kinds,
                               List<DirectoryConfigurationResolver> resolvers,
                               ApplicationEventPublisher events) {
        this.directories    = directories;
        this.configurations = configurations;
        this.kinds          = kinds;
        this.resolvers      = resolvers == null ? List.of() : List.copyOf(resolvers);
        this.events         = events;
    }

    /**
     * 🌳 One directory, with what applies to it.
     *
     * <p>⚠️ The enriched read, and deliberately not what a tree listing gives you: resolving an
     * effective rule walks a folder's ancestors, so filling it in per row would turn drawing a sidebar
     * into a resolve per node. A folder's own screen asks about one folder.</p>
     *
     * @param directoryId the directory
     * @return the directory and every kind of rule that applies to it
     */
    @Transactional(readOnly = true)
    public DirectoryView describe(String directoryId) {
        StorageDirectory directory = directories.require(directoryId);

        Map<String, DirectoryConfigurationView> applied = new LinkedHashMap<>();

        for (DirectoryConfigurationResolver resolver : resolvers) {
            applied.putAll(resolver.describe(directory));
        }

        return DirectoryView.of(directory, applied);
    }

    /**
     * 🌱 Every root a product declared for one owner.
     *
     * @param owner whose tree
     * @return the roots
     */
    @Transactional(readOnly = true)
    public List<StorageDirectory> roots(String owner) {
        return directories.roots(owner);
    }

    /**
     * 🌿 One directory and everything under it, in tree order.
     *
     * @param directoryId the directory
     * @return the subtree, itself first
     */
    @Transactional(readOnly = true)
    public List<StorageDirectory> subtree(String directoryId) {
        return directories.subtreeOf(directories.require(directoryId));
    }

    /**
     * 📁 Make a folder inside another.
     *
     * <p>⚠️ Resolving the parent and inserting under it are one unit: the insert renumbers against the
     * parent's range, and a parent read outside the boundary is a range that may already have moved.</p>
     *
     * @param parentId where it goes
     * @param name     what to call it
     * @return the new directory
     */
    @Transactional
    public StorageDirectory create(String parentId, String name) {
        return directories.create(directories.require(parentId), name);
    }

    /**
     * 🏷️ Rename one. Refused on a root by the tree itself.
     *
     * @param directoryId the directory
     * @param name        the new name
     * @return the renamed directory
     */
    @Transactional
    public StorageDirectory rename(String directoryId, String name) {
        return directories.rename(directories.require(directoryId), name);
    }

    /**
     * 📦 Move one under another.
     *
     * <p>⚠️ Both ends are resolved inside the boundary, and they must be: a move shifts two ranges, and
     * reading either of them before the transaction opens is reading a number that the other end's shift
     * is about to invalidate.</p>
     *
     * @param directoryId the directory to move
     * @param parentId    where it should go
     * @return the moved directory
     */
    @Transactional
    public StorageDirectory move(String directoryId, String parentId) {
        StorageDirectory directory   = directories.require(directoryId);
        StorageDirectory destination = directories.require(parentId);

        // ⚠️ BEFORE the move, while the subtree is still walkable at its old numbering — and the moved
        // branch now inherits from somewhere else entirely, which is the eviction that gets forgotten.
        forget(directory);

        return directories.moveTo(directory, destination);
    }

    /**
     * 🗑️ Remove one, and optionally the folders under it.
     *
     * @param directoryId the directory
     * @param withSubtree whether folders inside it go too
     */
    @Transactional
    public void delete(String directoryId, boolean withSubtree) {
        StorageDirectory directory = directories.require(directoryId);

        forget(directory);
        directories.delete(directory, withSubtree);
    }

    // ── What a folder says about itself ───────────────────────────────────────

    /**
     * 🔎 What this folder itself says, of one kind — never what it inherits.
     *
     * @param directoryId the folder
     * @param kind        which question, by name
     * @return the configuration, or empty when it carries none of its own
     */
    @Transactional(readOnly = true)
    public Optional<?> configuration(String directoryId, String kind) {
        directories.require(directoryId);

        return configurations().find(directoryId, requireKind(kind));
    }

    /**
     * 📋 Every kind this folder carries a row for.
     *
     * @param directoryId the folder
     * @return the kind names
     */
    @Transactional(readOnly = true)
    public List<String> configurationKinds(String directoryId) {
        directories.require(directoryId);

        return configurations().kindsOf(directoryId);
    }

    /**
     * ✏️ Say what this folder does, of one kind — replacing whatever it said before.
     *
     * <p>⚠️ Three things happen together and none of them is optional: the row is written, the cached
     * answers for the whole subtree are dropped, and the change is announced. A write without the
     * eviction leaves every folder beneath still resolving the old rule until a restart, which is a bug
     * that looks exactly like the feature not working.</p>
     *
     * <p>⚠️ The document is bound as <strong>that kind's record</strong> rather than stored as whatever
     * JSON arrived. A payload that will not bind is a refusal here, never a row that explodes at
     * somebody's next upload.</p>
     *
     * @param directoryId the folder
     * @param kind        which question, by name
     * @param document    the answer, as it arrived
     * @param <T>         the kind's payload type
     * @return the configuration as it now reads, normalised
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public <T> T writeConfiguration(String directoryId, String kind, Object document) {
        StorageDirectory              directory = directories.require(directoryId);
        DirectoryConfigurationKind<T> known     = (DirectoryConfigurationKind<T>) requireKind(kind);
        String                        previous  = payloadOf(directoryId, known);
        T                             payload   = configurations().bind(known, document);

        configurations().write(directoryId, known, payload);

        forget(directory);
        announce(directory, known.name(), previous, String.valueOf(payload));

        return payload;
    }

    /**
     * 🧹 Stop saying anything of this kind, and go back to inheriting.
     *
     * @param directoryId the folder
     * @param kind        which question, by name
     * @return whether there was anything to clear
     */
    @Transactional
    public boolean clearConfiguration(String directoryId, String kind) {
        StorageDirectory              directory = directories.require(directoryId);
        DirectoryConfigurationKind<?> known     = requireKind(kind);
        String                        previous  = payloadOf(directoryId, known);

        boolean cleared = configurations().clear(directoryId, known);

        forget(directory);

        if (cleared) {
            announce(directory, known.name(), previous, null);
        }

        return cleared;
    }

    /**
     * 📋 What kinds of configuration this installation knows about.
     *
     * @return the kind names
     */
    public List<String> knownKinds() {
        return kinds == null ? List.of() : kinds.names();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private StorageDirectoryConfigurations configurations() {
        if (configurations == null) {
            throw new IllegalStateException(
                "This installation stores no directory configurations — "
                + "declare a StorageDirectoryConfigurations bean to enable them.");
        }

        return configurations;
    }

    /**
     * 🔎 The kind of this name, or a refusal naming the ones that would have worked.
     */
    private DirectoryConfigurationKind<?> requireKind(String kind) {
        if (kinds == null) {
            throw new IllegalStateException("This installation registers no configuration kinds.");
        }

        return kinds.require(kind);
    }

    /**
     * 📜 The stored document as it currently reads, for an audit line to compare against.
     */
    private String payloadOf(String directoryId, DirectoryConfigurationKind<?> kind) {
        return configurations().find(directoryId, kind).map(Object::toString).orElse(null);
    }

    /**
     * 🧹 Drop whatever was memoised about this folder and everything under it.
     */
    private void forget(StorageDirectory directory) {
        for (DirectoryConfigurationResolver resolver : resolvers) {
            resolver.evictSubtreeOf(directory);
        }
    }

    private void announce(StorageDirectory directory, String kind, String previous, String payload) {
        if (events != null) {
            events.publishEvent(new FileManagementEvent.ConfigurationChanged(
                    directory.getId(), directory.getPath(), kind, previous, payload));
        }
    }
}
