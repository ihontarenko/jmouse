package org.jmouse.taxonomy.management;

import java.util.List;
import java.util.Set;

/**
 * 🧾 What the taxonomy routes answer and take. A category is always named by its path, never by an id.
 */
public final class TaxonomyViews {

    private TaxonomyViews() {
    }

    /** A taxonomy: how it is named, and the entity types it classifies. */
    public record TaxonomyView(String code, String title, String description, Set<String> entityTypes) {
    }

    /**
     * One category, in tree order.
     *
     * @param parent the path of the category above, {@code null} at a root
     * @param count  the distinct entities the reader may see in it and everything beneath it
     */
    public record CategoryView(String path, String parent, String title, String description, int depth, long count) {
    }

    /** A taxonomy and every one of its categories, each followed by what is beneath it. */
    public record TreeView(TaxonomyView taxonomy, List<CategoryView> categories) {
    }

    /** One filing: an entity in a category. */
    public record AssignmentView(String path, String entityType, String entityId) {
    }

    /** An entity filed in a category, with what its host calls it. */
    public record FiledEntityView(String entityType, String entityId, String title) {
    }

    /**
     * A category an entity is filed in.
     *
     * @param trail the titles from the root down to the category itself
     */
    public record EntityCategoryView(String taxonomy, String taxonomyTitle, String path, List<String> trail) {
    }

    /**
     * A new category.
     *
     * @param parent the path it goes beneath; {@code null} or empty for a root
     */
    public record CategoryRequest(String parent, String title, String description) {
    }

    /** A category renamed or redescribed. */
    public record CategoryChange(String title, String description) {
    }

    /**
     * A category moved.
     *
     * @param destination the path it goes beneath; {@code null} or empty to make it a root
     */
    public record MoveRequest(String path, String destination) {
    }

    /** An entity filed in, or taken out of, a category. */
    public record AssignmentRequest(String path, String entityType, String entityId) {
    }
}
