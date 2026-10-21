package org.jmouse.taxonomy.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jmouse.taxonomy.CategoryPath;
import org.jmouse.tree.NestedSetNode;

import java.time.LocalDateTime;

/**
 * 🌿 One category of a taxonomy: a node of its nested set, addressed by its path of slugs.
 *
 * <p>The numbering and every write that keeps it true are {@code jmouse-tree}'s; the tree a category belongs to is
 * its taxonomy ({@link #getTaxonomyId()}), so two taxonomies are numbered apart. What makes it a category — a title,
 * a slug, a path unique within its taxonomy — is kept here and by {@link Taxonomies}.</p>
 *
 * <p>⚠️ The path is denormalised on purpose, as a storage directory's is: a host naming {@code kicad/device} finds it
 * with one indexed lookup, and the unique constraint is what stops two categories of one name beside each other. A
 * move or a rename rewrites the paths of the whole subtree.</p>
 */
@Entity
@Table(
    name = "taxonomy_categories",
    uniqueConstraints = @UniqueConstraint(name = "unique_taxonomy_categories_path", columnNames = {"taxonomy_id", "path"})
)
public class TaxonomyCategory implements NestedSetNode {

    /** How long a path may be: several levels of slugs, each up to a title's length. */
    public static final int MAXIMUM_PATH_LENGTH = 512;

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "taxonomy_id", length = 36, nullable = false, updatable = false)
    private String taxonomyId;

    /** Null at a root. The shape of the tree, and what the numbering is rebuilt from. */
    @Column(name = "parent_id", length = 36)
    private String parentId;

    @Column(name = "slug", length = 160, nullable = false)
    private String slug;

    @Column(name = "path", length = MAXIMUM_PATH_LENGTH, nullable = false)
    private String path;

    @Column(name = "title", length = 160, nullable = false)
    private String title;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "tree_left", nullable = false)
    private int treeLeft;

    @Column(name = "tree_right", nullable = false)
    private int treeRight;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TaxonomyCategory() {
    }

    public TaxonomyCategory(String id, String taxonomyId, String parentId, String title, String description,
                            CategoryPath path, int treeLeft, int treeRight, int depth, int sortOrder) {
        this.id = id;
        this.taxonomyId = taxonomyId;
        this.parentId = parentId;
        this.title = title;
        this.description = description;
        this.slug = path.slug();
        this.path = path.toString();
        this.treeLeft = treeLeft;
        this.treeRight = treeRight;
        this.depth = depth;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void created() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void updated() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getTaxonomyId() {
        return taxonomyId;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    @Override
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getSlug() {
        return slug;
    }

    public CategoryPath getPath() {
        return CategoryPath.parse(path);
    }

    /** Where it now stands: its slug is the path's last, every category beneath it follows. */
    void setPath(CategoryPath path) {
        this.path = path.toString();
        this.slug = path.slug();
    }

    public String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    @Override
    public int getTreeLeft() {
        return treeLeft;
    }

    @Override
    public void setTreeLeft(int treeLeft) {
        this.treeLeft = treeLeft;
    }

    @Override
    public int getTreeRight() {
        return treeRight;
    }

    @Override
    public void setTreeRight(int treeRight) {
        this.treeRight = treeRight;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public int getSortOrder() {
        return sortOrder;
    }

    void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
