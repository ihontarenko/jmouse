package org.jmouse.taxonomy.management;

import org.jmouse.taxonomy.CategoryPath;
import org.jmouse.taxonomy.EntityType;
import org.jmouse.taxonomy.TaxonomyException;
import org.jmouse.taxonomy.jpa.Taxonomies;
import org.jmouse.taxonomy.jpa.Taxonomy;
import org.jmouse.taxonomy.jpa.TaxonomyAssignment;
import org.jmouse.taxonomy.jpa.TaxonomyCategory;
import org.jmouse.taxonomy.management.TaxonomyViews.AssignmentView;
import org.jmouse.taxonomy.management.TaxonomyViews.CategoryView;
import org.jmouse.taxonomy.management.TaxonomyViews.EntityCategoryView;
import org.jmouse.taxonomy.management.TaxonomyViews.FiledEntityView;
import org.jmouse.taxonomy.management.TaxonomyViews.TaxonomyView;
import org.jmouse.taxonomy.management.TaxonomyViews.TreeView;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🗂️ Taxonomies as their routes serve them: every write in a transaction, every answer narrowed to what the reader may
 * see.
 *
 * <h3>⚠️ What a reader sees is each entity type's answer</h3>
 *
 * <p>A count, a list of filed entities, the assignments a shelf filters by — each is narrowed through the entity type's
 * {@link EntityType#titlesOf}, which leaves out an entity the reader may not see. A category's count is therefore of
 * the entities this reader may see in it, never of every row: a count of someone else's private objects would say
 * they exist.</p>
 */
public class TaxonomyManagement {

    private final Taxonomies taxonomies;

    public TaxonomyManagement(Taxonomies taxonomies) {
        this.taxonomies = taxonomies;
    }

    @Transactional(readOnly = true)
    public List<TaxonomyView> taxonomies() {
        return taxonomies.taxonomies().stream().map(TaxonomyManagement::viewOf).toList();
    }

    /**
     * 🌳 A taxonomy's categories in tree order, each counting what the reader may see in it and beneath it.
     *
     * @param entityType count only entities of this type; {@code null} for every type the taxonomy classifies
     */
    @Transactional(readOnly = true)
    public TreeView tree(String taxonomyCode, String entityType) {
        Taxonomy taxonomy = taxonomies.requireTaxonomy(taxonomyCode);
        List<TaxonomyCategory> categories = taxonomies.categories(taxonomyCode);
        // Which categories each entity the reader may see is filed in. Visibility is the entity type's answer, not a
        // column, so the filings are read once and narrowed here rather than counted by the database.
        Map<String, Set<String>> filingsOf = new HashMap<>();
        for (TaxonomyAssignment assignment : visible(taxonomies.assignments(taxonomyCode, entityType))) {
            filingsOf.computeIfAbsent(assignment.getEntityType() + ":" + assignment.getEntityId(), ignored -> new HashSet<>())
                    .add(assignment.getCategoryId());
        }
        // An entity counts once in every category on the way up from where it is filed: filed in two siblings, it is
        // one entity of their parent. Each walk is as long as the tree is deep.
        Map<String, TaxonomyCategory> byId = new HashMap<>();
        categories.forEach(category -> byId.put(category.getId(), category));
        Map<String, Long> counts = new HashMap<>();
        for (Set<String> filedIn : filingsOf.values()) {
            Set<String> holding = new HashSet<>();
            for (String categoryId : filedIn) {
                for (TaxonomyCategory step = byId.get(categoryId); step != null && holding.add(step.getId());
                     step = step.getParentId() == null ? null : byId.get(step.getParentId())) {
                    counts.merge(step.getId(), 1L, Long::sum);
                }
            }
        }
        List<CategoryView> views = new ArrayList<>();
        for (TaxonomyCategory category : categories) {
            views.add(viewOf(category, counts.getOrDefault(category.getId(), 0L)));
        }
        return new TreeView(viewOf(taxonomy), views);
    }

    /** Every filing of a taxonomy the reader may see: what a shelf filters by. */
    @Transactional(readOnly = true)
    public List<AssignmentView> assignments(String taxonomyCode, String entityType) {
        Map<String, String> paths = taxonomies.categories(taxonomyCode).stream()
                .collect(Collectors.toMap(TaxonomyCategory::getId, category -> category.getPath().toString()));
        return visible(taxonomies.assignments(taxonomyCode, entityType)).stream()
                .map(assignment -> new AssignmentView(paths.get(assignment.getCategoryId()), assignment.getEntityType(),
                        assignment.getEntityId()))
                .toList();
    }

    /** What the reader may see filed in a category — or in it and everything beneath it — with what each is called. */
    @Transactional(readOnly = true)
    public List<FiledEntityView> entities(String taxonomyCode, String path, String entityType, boolean withSubtree) {
        List<TaxonomyAssignment> filed = taxonomies.entitiesOf(taxonomyCode, CategoryPath.parse(path), entityType, withSubtree);
        Map<String, Map<String, String>> titles = titlesByType(filed);
        Map<String, FiledEntityView> unique = new LinkedHashMap<>();
        for (TaxonomyAssignment assignment : filed) {
            String title = titles.getOrDefault(assignment.getEntityType(), Map.of()).get(assignment.getEntityId());
            if (title != null) {
                unique.putIfAbsent(assignment.getEntityType() + ":" + assignment.getEntityId(),
                        new FiledEntityView(assignment.getEntityType(), assignment.getEntityId(), title));
            }
        }
        return List.copyOf(unique.values());
    }

    /** Every category an entity is filed in, across every taxonomy, each with its trail of titles. */
    @Transactional(readOnly = true)
    public List<EntityCategoryView> categoriesOf(String entityType, String entityId) {
        EntityType type = taxonomies.entityTypes().require(entityType);
        if (!type.titlesOf(List.of(entityId)).containsKey(entityId)) {
            throw new TaxonomyException.NotFound("There is no " + entityType + " '" + entityId + "' to read the categories of.");
        }
        Map<String, Taxonomy> byId = taxonomies.taxonomies().stream().collect(Collectors.toMap(Taxonomy::getId, taxonomy -> taxonomy));
        List<EntityCategoryView> views = new ArrayList<>();
        for (TaxonomyCategory category : taxonomies.categoriesOf(entityType, entityId)) {
            Taxonomy taxonomy = byId.get(category.getTaxonomyId());
            List<String> trail = taxonomies.ancestorsOf(category).stream().map(TaxonomyCategory::getTitle).toList();
            List<String> withItself = new ArrayList<>(trail);
            if (withItself.isEmpty() || !withItself.getLast().equals(category.getTitle())) {
                withItself.add(category.getTitle());
            }
            views.add(new EntityCategoryView(taxonomy.getCode(), taxonomy.getTitle(), category.getPath().toString(),
                    withItself));
        }
        return views;
    }

    @Transactional
    public CategoryView createCategory(String taxonomyCode, String parent, String title, String description) {
        TaxonomyCategory category = taxonomies.createCategory(taxonomyCode, pathOrRoot(parent), title, description);
        return viewOf(category, 0);
    }

    @Transactional
    public CategoryView changeCategory(String taxonomyCode, String path, String title, String description) {
        return viewOf(taxonomies.renameCategory(taxonomyCode, CategoryPath.parse(path), title, description), 0);
    }

    @Transactional
    public CategoryView moveCategory(String taxonomyCode, String path, String destination) {
        return viewOf(taxonomies.moveCategory(taxonomyCode, CategoryPath.parse(path), pathOrRoot(destination)), 0);
    }

    @Transactional
    public void deleteCategory(String taxonomyCode, String path, boolean withSubtree) {
        taxonomies.deleteCategory(taxonomyCode, CategoryPath.parse(path), withSubtree);
    }

    /**
     * 📌 An entity filed in a category.
     *
     * @throws TaxonomyException.NotFound when the reader may not see the entity: nobody files what they cannot see
     */
    @Transactional
    public AssignmentView assign(String taxonomyCode, String path, String entityType, String entityId) {
        requireVisible(entityType, entityId);
        TaxonomyAssignment assignment = taxonomies.assign(taxonomyCode, CategoryPath.parse(path), entityType, entityId);
        return new AssignmentView(path, assignment.getEntityType(), assignment.getEntityId());
    }

    @Transactional
    public void unassign(String taxonomyCode, String path, String entityType, String entityId) {
        taxonomies.unassign(taxonomyCode, CategoryPath.parse(path), entityType, entityId);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /** The filings whose entity the reader may see, one question per entity type. */
    private List<TaxonomyAssignment> visible(List<TaxonomyAssignment> filed) {
        Map<String, Map<String, String>> titles = titlesByType(filed);
        return filed.stream()
                .filter(assignment -> titles.getOrDefault(assignment.getEntityType(), Map.of()).containsKey(assignment.getEntityId()))
                .toList();
    }

    private Map<String, Map<String, String>> titlesByType(List<TaxonomyAssignment> filed) {
        Map<String, Set<String>> idsByType = new LinkedHashMap<>();
        for (TaxonomyAssignment assignment : filed) {
            idsByType.computeIfAbsent(assignment.getEntityType(), ignored -> new HashSet<>()).add(assignment.getEntityId());
        }
        Map<String, Map<String, String>> titles = new HashMap<>();
        idsByType.forEach((type, ids) -> taxonomies.entityTypes().find(type)
                .ifPresent(entityType -> titles.put(type, entityType.titlesOf(ids))));
        return titles;
    }

    private void requireVisible(String entityType, String entityId) {
        if (!taxonomies.entityTypes().require(entityType).titlesOf(List.of(entityId)).containsKey(entityId)) {
            throw new TaxonomyException.NotFound("There is no " + entityType + " '" + entityId + "' to file.");
        }
    }

    private static CategoryPath pathOrRoot(String written) {
        return written == null || written.isBlank() ? null : CategoryPath.parse(written);
    }

    private static TaxonomyView viewOf(Taxonomy taxonomy) {
        return new TaxonomyView(taxonomy.getCode(), taxonomy.getTitle(), taxonomy.getDescription(), taxonomy.getEntityTypes());
    }

    private static CategoryView viewOf(TaxonomyCategory category, long count) {
        CategoryPath parent = category.getPath().parent();
        return new CategoryView(category.getPath().toString(), parent == null ? null : parent.toString(), category.getTitle(),
                category.getDescription(), category.getDepth(), count);
    }
}
