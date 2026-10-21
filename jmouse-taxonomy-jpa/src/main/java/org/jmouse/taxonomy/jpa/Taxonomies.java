package org.jmouse.taxonomy.jpa;

import jakarta.persistence.EntityManager;
import org.jmouse.taxonomy.CategoryPath;
import org.jmouse.taxonomy.EntityTypes;
import org.jmouse.taxonomy.TaxonomyException;
import org.jmouse.tree.TreeException;
import org.jmouse.tree.jpa.NestedSetMapping;
import org.jmouse.tree.jpa.NestedSetTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 🗂️ Taxonomies, their categories and what is filed in them.
 *
 * <p>A category is a node of its taxonomy's nested set ({@code jmouse-tree}); this class keeps what makes it a
 * category — a title, a slug, a path unique within its taxonomy — and the assignments of registered entities to it.
 * Categories are addressed by taxonomy code and path ({@code catalogue}, {@code semiconductors/diodes}), never by id,
 * so a host's seed and a person's link name them alike.</p>
 *
 * <h3>⚠️ Every write runs inside the caller's transaction</h3>
 *
 * <p>None is opened here, as the tree opens none: a renumbering is only safe inside one, and a host filing a hundred
 * entities on start wants them in one. Like the tree, a write clears the persistence context; anything a caller loaded
 * before it is stale.</p>
 */
public class Taxonomies {

    /** Siblings stand in the order they were made, then by title — a renumbering never shuffles them. */
    private static final Comparator<TaxonomyCategory> SIBLING_ORDER = Comparator
            .comparingInt(TaxonomyCategory::getSortOrder)
            .thenComparing(TaxonomyCategory::getTitle);

    private final EntityManager entityManager;
    private final Supplier<String> identifiers;
    private final EntityTypes entityTypes;
    private final NestedSetTree<TaxonomyCategory> tree;

    public Taxonomies(EntityManager entityManager, Supplier<String> identifiers, EntityTypes entityTypes) {
        this.entityManager = entityManager;
        this.identifiers = identifiers;
        this.entityTypes = entityTypes;
        this.tree = new NestedSetTree<>(entityManager, new NestedSetMapping<>(TaxonomyCategory.class, "taxonomyId",
                TaxonomyCategory::getTaxonomyId, SIBLING_ORDER));
    }

    public EntityTypes entityTypes() {
        return entityTypes;
    }

    // ── Taxonomies ────────────────────────────────────────────────────────────

    public List<Taxonomy> taxonomies() {
        return entityManager.createQuery("SELECT taxonomy FROM Taxonomy taxonomy ORDER BY taxonomy.title", Taxonomy.class)
                .getResultList();
    }

    public Optional<Taxonomy> findTaxonomy(String code) {
        return entityManager.createQuery("SELECT taxonomy FROM Taxonomy taxonomy WHERE taxonomy.code = :code", Taxonomy.class)
                .setParameter("code", code)
                .getResultStream()
                .findFirst();
    }

    public Taxonomy requireTaxonomy(String code) {
        return findTaxonomy(code).orElseThrow(() -> new TaxonomyException.NotFound("There is no taxonomy '" + code + "'."));
    }

    /**
     * 🗂️ A taxonomy as a host declares it: made when it is not there, its title, description and entity types set
     * when it is. What a host seeds on every start, so calling it twice changes nothing.
     *
     * @throws TaxonomyException.Invalid for an entity type nobody registered
     */
    public Taxonomy defineTaxonomy(String code, String title, String description, Set<String> types) {
        String slug = CategoryPath.slugOf(code);
        if (!slug.equals(code)) {
            throw new TaxonomyException.Invalid("A taxonomy's code is a slug, '" + slug + "', not '" + code + "'.");
        }
        types.forEach(entityTypes::require);
        requireTitle(title);
        Optional<Taxonomy> existing = findTaxonomy(code);
        if (existing.isPresent()) {
            Taxonomy taxonomy = existing.get();
            taxonomy.setTitle(title);
            taxonomy.setDescription(description);
            taxonomy.setEntityTypes(types);
            return taxonomy;
        }
        Taxonomy taxonomy = new Taxonomy(identifiers.get(), code, title, description, types);
        entityManager.persist(taxonomy);
        return taxonomy;
    }

    // ── Categories ────────────────────────────────────────────────────────────

    /** Every category of a taxonomy, in tree order: each followed by everything beneath it. */
    public List<TaxonomyCategory> categories(String taxonomyCode) {
        return entityManager.createQuery("SELECT category FROM TaxonomyCategory category WHERE category.taxonomyId = :taxonomy "
                        + "ORDER BY category.treeLeft", TaxonomyCategory.class)
                .setParameter("taxonomy", requireTaxonomy(taxonomyCode).getId())
                .getResultList();
    }

    public Optional<TaxonomyCategory> findCategory(String taxonomyCode, CategoryPath path) {
        return findCategory(requireTaxonomy(taxonomyCode), path);
    }

    public TaxonomyCategory requireCategory(String taxonomyCode, CategoryPath path) {
        return findCategory(taxonomyCode, path).orElseThrow(() -> new TaxonomyException.NotFound(
                "The taxonomy '" + taxonomyCode + "' has no category '" + path + "'."));
    }

    /** The category and everything above it, root first. */
    public List<TaxonomyCategory> ancestorsOf(TaxonomyCategory category) {
        return tree.ancestorsOf(category);
    }

    /**
     * 🌱 A new category, the last beneath its parent — or a new root when there is none.
     *
     * @param parent the category it goes beneath; {@code null} for a root
     * @throws TaxonomyException.Conflict when one of the same slug stands there already
     */
    public TaxonomyCategory createCategory(String taxonomyCode, CategoryPath parent, String title, String description) {
        Taxonomy taxonomy = requireTaxonomy(taxonomyCode);
        requireTitle(title);
        CategoryPath path = parent == null ? new CategoryPath(List.of(CategoryPath.slugOf(title)))
                : parent.child(CategoryPath.slugOf(title));
        requireFree(taxonomy, path);
        if (parent == null) {
            return tree.insertRoot(taxonomy.getId(), place -> new TaxonomyCategory(identifiers.get(), taxonomy.getId(), null,
                    title.strip(), description, path, place.treeLeft(), place.treeRight(), place.depth(), place.sortOrder()));
        }
        TaxonomyCategory above = requireCategory(taxonomyCode, parent);
        return tree.insertUnder(above, place -> new TaxonomyCategory(identifiers.get(), taxonomy.getId(), above.getId(),
                title.strip(), description, path, place.treeLeft(), place.treeRight(), place.depth(), place.sortOrder()));
    }

    /**
     * 🧭 The category these titles name from the root down, made level by level where a level is missing — how a host
     * files into {@code KiCad / Device} without creating it first.
     */
    public TaxonomyCategory ensureCategory(String taxonomyCode, List<String> titles) {
        if (titles.isEmpty()) {
            throw new TaxonomyException.Invalid("A category is named by at least its own title.");
        }
        Taxonomy taxonomy = requireTaxonomy(taxonomyCode);
        CategoryPath parent = null;
        TaxonomyCategory category = null;
        for (String title : titles) {
            CategoryPath path = parent == null ? new CategoryPath(List.of(CategoryPath.slugOf(title)))
                    : parent.child(CategoryPath.slugOf(title));
            Optional<TaxonomyCategory> found = findCategory(taxonomy, path);
            category = found.isPresent() ? found.get() : createCategory(taxonomyCode, parent, title, null);
            parent = path;
        }
        return category;
    }

    /**
     * ✏️ A category called something else. Its slug follows the title, and so does the path of everything beneath it.
     *
     * @throws TaxonomyException.Conflict when the new slug is taken beside it
     */
    public TaxonomyCategory renameCategory(String taxonomyCode, CategoryPath path, String title, String description) {
        Taxonomy taxonomy = requireTaxonomy(taxonomyCode);
        requireTitle(title);
        TaxonomyCategory category = requireCategory(taxonomyCode, path);
        CategoryPath renamed = path.parent() == null ? new CategoryPath(List.of(CategoryPath.slugOf(title)))
                : path.parent().child(CategoryPath.slugOf(title));
        if (!renamed.equals(path)) {
            requireFree(taxonomy, renamed);
            repath(category, path, renamed);
        }
        category.setTitle(title.strip());
        category.setDescription(description);
        entityManager.flush();
        entityManager.clear();
        return requireCategory(taxonomyCode, renamed);
    }

    /**
     * 📦 A category moved, with everything beneath it, under another — or made a root when {@code destination} is
     * {@code null}.
     *
     * @throws TaxonomyException.Invalid when the destination is the category or beneath it
     * @throws TaxonomyException.Conflict when one of its slug stands at the destination already
     */
    public TaxonomyCategory moveCategory(String taxonomyCode, CategoryPath path, CategoryPath destination) {
        Taxonomy taxonomy = requireTaxonomy(taxonomyCode);
        TaxonomyCategory category = requireCategory(taxonomyCode, path);
        CategoryPath moved = destination == null ? new CategoryPath(List.of(path.slug())) : destination.child(path.slug());
        if (moved.equals(path)) {
            return category;
        }
        requireFree(taxonomy, moved);
        if (destination == null) {
            repath(category, path, moved);
            category.setParentId(null);
            entityManager.flush();
            tree.renumber(taxonomy.getId());
            entityManager.clear();
            return requireCategory(taxonomyCode, moved);
        }
        TaxonomyCategory target = requireCategory(taxonomyCode, destination);
        if (target.getId().equals(category.getId()) || category.contains(target)) {
            throw new TaxonomyException.Invalid("'" + path + "' cannot be moved into itself or beneath itself.");
        }
        repath(category, path, moved);
        try {
            tree.move(category, target);
        } catch (TreeException refused) {
            throw new TaxonomyException.Invalid(refused.getMessage());
        }
        return requireCategory(taxonomyCode, moved);
    }

    /**
     * 🗑️ A category gone. With {@code withSubtree} everything beneath it and every assignment in them go too;
     * without, a category holding anything is refused.
     *
     * @throws TaxonomyException.Conflict when it holds categories or entities and {@code withSubtree} is off
     */
    public void deleteCategory(String taxonomyCode, CategoryPath path, boolean withSubtree) {
        TaxonomyCategory category = requireCategory(taxonomyCode, path);
        if (!withSubtree) {
            boolean hasChildren = category.getTreeRight() - category.getTreeLeft() > 1;
            long filed = entityManager.createQuery("SELECT COUNT(assignment) FROM TaxonomyAssignment assignment "
                            + "WHERE assignment.categoryId = :category", Long.class)
                    .setParameter("category", category.getId())
                    .getSingleResult();
            if (hasChildren || filed > 0) {
                throw new TaxonomyException.Conflict("'" + path + "' holds " + (hasChildren ? "categories" : filed
                        + " filed entities") + "; delete it with everything in it, or empty it first.");
            }
        }
        List<String> removed = tree.subtreeOf(category).stream().map(TaxonomyCategory::getId).toList();
        entityManager.createQuery("DELETE FROM TaxonomyAssignment assignment WHERE assignment.categoryId IN :categories")
                .setParameter("categories", removed)
                .executeUpdate();
        tree.delete(entityManager.find(TaxonomyCategory.class, category.getId()));
    }

    // ── Assignments ───────────────────────────────────────────────────────────

    /**
     * 📌 An entity filed in a category. Filing it where it already is changes nothing.
     *
     * @throws TaxonomyException.Invalid for an entity type nobody registered, or one this taxonomy does not classify
     */
    public TaxonomyAssignment assign(String taxonomyCode, CategoryPath path, String entityType, String entityId) {
        Taxonomy taxonomy = requireTaxonomy(taxonomyCode);
        requireClassifies(taxonomy, entityType);
        requireEntityId(entityId);
        TaxonomyCategory category = requireCategory(taxonomyCode, path);
        Optional<TaxonomyAssignment> existing = assignmentOf(category.getId(), entityType, entityId);
        if (existing.isPresent()) {
            return existing.get();
        }
        TaxonomyAssignment assignment = new TaxonomyAssignment(identifiers.get(), taxonomy.getId(), category.getId(),
                entityType, entityId);
        entityManager.persist(assignment);
        return assignment;
    }

    /** An entity taken out of a category; one that was not filed there leaves nothing to do. */
    public void unassign(String taxonomyCode, CategoryPath path, String entityType, String entityId) {
        TaxonomyCategory category = requireCategory(taxonomyCode, path);
        assignmentOf(category.getId(), entityType, entityId).ifPresent(entityManager::remove);
    }

    /** Every category an entity is filed in, across every taxonomy. */
    public List<TaxonomyCategory> categoriesOf(String entityType, String entityId) {
        entityTypes.require(entityType);
        return entityManager.createQuery("SELECT category FROM TaxonomyCategory category, TaxonomyAssignment assignment "
                        + "WHERE assignment.categoryId = category.id AND assignment.entityType = :type "
                        + "AND assignment.entityId = :id ORDER BY category.taxonomyId, category.treeLeft", TaxonomyCategory.class)
                .setParameter("type", entityType)
                .setParameter("id", entityId)
                .getResultList();
    }

    /**
     * 📚 What is filed in a category — or in it and everything beneath it.
     *
     * @param entityType only entities of this type; {@code null} for every type
     */
    public List<TaxonomyAssignment> entitiesOf(String taxonomyCode, CategoryPath path, String entityType, boolean withSubtree) {
        TaxonomyCategory category = requireCategory(taxonomyCode, path);
        List<String> categories = withSubtree ? tree.subtreeOf(category).stream().map(TaxonomyCategory::getId).toList()
                : List.of(category.getId());
        return entityManager.createQuery("SELECT assignment FROM TaxonomyAssignment assignment "
                        + "WHERE assignment.categoryId IN :categories AND (:type IS NULL OR assignment.entityType = :type) "
                        + "ORDER BY assignment.entityType, assignment.entityId", TaxonomyAssignment.class)
                .setParameter("categories", categories)
                .setParameter("type", entityType)
                .getResultList();
    }

    /** Every assignment of a taxonomy, of one entity type or of every type: what a shelf filters by. */
    public List<TaxonomyAssignment> assignments(String taxonomyCode, String entityType) {
        return entityManager.createQuery("SELECT assignment FROM TaxonomyAssignment assignment "
                        + "WHERE assignment.taxonomyId = :taxonomy AND (:type IS NULL OR assignment.entityType = :type)",
                        TaxonomyAssignment.class)
                .setParameter("taxonomy", requireTaxonomy(taxonomyCode).getId())
                .setParameter("type", entityType)
                .getResultList();
    }

    /**
     * 🧹 Every assignment of these entities removed — what a host does when it deletes them, so no category counts
     * an entity that is gone.
     */
    public int forget(String entityType, Collection<String> entityIds) {
        if (entityIds.isEmpty()) {
            return 0;
        }
        return entityManager.createQuery("DELETE FROM TaxonomyAssignment assignment WHERE assignment.entityType = :type "
                        + "AND assignment.entityId IN :ids")
                .setParameter("type", entityType)
                .setParameter("ids", entityIds)
                .executeUpdate();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private Optional<TaxonomyCategory> findCategory(Taxonomy taxonomy, CategoryPath path) {
        return entityManager.createQuery("SELECT category FROM TaxonomyCategory category "
                        + "WHERE category.taxonomyId = :taxonomy AND category.path = :path", TaxonomyCategory.class)
                .setParameter("taxonomy", taxonomy.getId())
                .setParameter("path", path.toString())
                .getResultStream()
                .findFirst();
    }

    private Optional<TaxonomyAssignment> assignmentOf(String categoryId, String entityType, String entityId) {
        return entityManager.createQuery("SELECT assignment FROM TaxonomyAssignment assignment WHERE assignment.categoryId = "
                        + ":category AND assignment.entityType = :type AND assignment.entityId = :id", TaxonomyAssignment.class)
                .setParameter("category", categoryId)
                .setParameter("type", entityType)
                .setParameter("id", entityId)
                .getResultStream()
                .findFirst();
    }

    /** The category and everything beneath it re-addressed: {@code from/…} becomes {@code to/…}. */
    private void repath(TaxonomyCategory category, CategoryPath from, CategoryPath to) {
        List<TaxonomyCategory> subtree = new ArrayList<>(tree.subtreeOf(category));
        if (subtree.stream().noneMatch(node -> node.getId().equals(category.getId()))) {
            subtree.add(category);
        }
        for (TaxonomyCategory node : subtree) {
            List<String> slugs = node.getPath().slugs();
            List<String> tail = slugs.subList(from.depth(), slugs.size());
            List<String> rewritten = new ArrayList<>(to.slugs());
            rewritten.addAll(tail);
            TaxonomyCategory managed = node.getId().equals(category.getId()) ? category
                    : entityManager.find(TaxonomyCategory.class, node.getId());
            managed.setPath(new CategoryPath(rewritten));
        }
    }

    private void requireFree(Taxonomy taxonomy, CategoryPath path) {
        if (findCategory(taxonomy, path).isPresent()) {
            throw new TaxonomyException.Conflict("The taxonomy '" + taxonomy.getCode() + "' has a category '" + path
                    + "' already.");
        }
    }

    private void requireClassifies(Taxonomy taxonomy, String entityType) {
        entityTypes.require(entityType);
        if (!taxonomy.classifies(entityType)) {
            throw new TaxonomyException.Invalid("The taxonomy '" + taxonomy.getCode() + "' classifies "
                    + taxonomy.getEntityTypes() + ", not '" + entityType + "'.");
        }
    }

    private static void requireTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > 160) {
            throw new TaxonomyException.Invalid("A title is between one and 160 characters.");
        }
    }

    private static void requireEntityId(String entityId) {
        if (entityId == null || entityId.isBlank() || entityId.length() > 128) {
            throw new TaxonomyException.Invalid("An entity is named by an id of one to 128 characters.");
        }
    }
}
