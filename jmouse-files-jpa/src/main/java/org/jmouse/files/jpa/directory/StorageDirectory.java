package org.jmouse.files.jpa.directory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jmouse.files.directory.DirectoryPath;

import java.time.LocalDateTime;

/**
 * 🌳 A folder in the library's own tree — the one files are filed into.
 *
 * <h3>⚠️ Not a product's categories, and deliberately a second tree</h3>
 *
 * <p>Innoventa and Kiwi each have a {@code categories} table, and both keep it: those organise
 * <em>pages</em>, and a page tree is a product's own idea about its own content. This tree organises
 * files and belongs to the library, so a product ends up running two hierarchies side by side. That
 * is a deliberate arrangement rather than a duplication — they hold different things — and it is why
 * this table is called {@code storage_directories}. Naming it {@code categories} would collide with a
 * table all three products already have and kill the first migration.</p>
 *
 * <h3>⚠️ Roots are named {@code <application>/<purpose>}, and cannot move</h3>
 *
 * <p>{@code innoventa/files}, {@code innoventa/avatars}, {@code tessera/attachments}. A root's path is
 * handed to the storage key as its namespace, so renaming or re-parenting one would leave every key
 * already written disagreeing with the tree. {@link #root} marks them, and the service refuses to
 * move or rename one.</p>
 *
 * <p>⚠️ Directories <em>below</em> a root contribute nothing to a key, for the mirror-image reason:
 * moving a file between folders has to be a row changing, never bytes being copied.</p>
 *
 * <h3>⚠️ A real nested set, and it is load-bearing</h3>
 *
 * <p>{@link #treeLeft} and {@link #treeRight} sit <em>beside</em> {@link #parentId} rather than
 * instead of it. The numbering is what makes "everything under this directory" one indexed range
 * query — which an authorization engine granting access to a subtree asks on every read, and which a
 * parent pointer turns into a recursive descent. The pointer stays and is the authority: a move is
 * <em>expressed</em> as a new parent and <em>executed</em> as a renumbering, so a numbering that
 * somehow ends up corrupt can always be rebuilt from the pointers.</p>
 */
@Entity
@Table(
    name = "storage_directories",
    uniqueConstraints = @UniqueConstraint(name = "unique_storage_directories_path",
                                          columnNames = {"owner_key", "path"})
)
public class StorageDirectory {

    /**
     * How deep the tree may go, counting a root as level one.
     *
     * <p>Not a storage limit — nothing in the schema cares. It is a readability limit, and it is
     * generous because the first two levels are always spent on the root's application and purpose.</p>
     */
    public static final int MAXIMUM_DEPTH = 8;

    /**
     * Owner key of a tree that belongs to the installation rather than to anybody in it.
     *
     * <p>⚠️ Not a kind-and-identifier like every other owner key, because there is nothing to identify.
     * It cannot collide with one: no product's {@code KIND:id} is a bare asterisk.</p>
     */
    public static final String INSTALLATION = "*";

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    /**
     * Whose tree this belongs to, or {@link #INSTALLATION} when it belongs to nobody in particular.
     *
     * <p>⚠️ <strong>A sentinel rather than NULL, and that is not fussiness.</strong> The uniqueness that
     * matters is {@code (owner_key, path)}, and neither MySQL nor PostgreSQL treats two NULLs as equal —
     * so with NULL for "the installation", the one case where a duplicate root is most likely would be
     * the one case the database never checked. Both products that grew a tree of their own wrote this
     * down after meeting it.</p>
     *
     * <p>Kiwi and Tessera keep {@link #INSTALLATION}: their files belong to sections and issues, not to
     * people. Innoventa carries an account, because its file cabinet is genuinely personal — every user
     * has their own folders, and collapsing them into one shared tree would be a different product.</p>
     *
     * <h4>⚠️ Write it as {@code KIND:id}, never as a bare identifier</h4>
     *
     * <p>The library never interprets this value — it is an equality key and nothing more, never resolved
     * to a person and never authorized on. Which is exactly why the <em>kind</em> has to be in it: a
     * product with two sorts of owner (a user and a workspace, a member and an agent) writing bare
     * identifiers has no way to tell them apart, and two id spaces that happen to overlap would silently
     * share a tree.</p>
     *
     * <p>{@link org.jmouse.files.OwnerReference#toString()} already produces exactly that form, and it is
     * the same shape {@link org.jmouse.files.jpa.FileBinding} carries as two columns. One string here
     * rather than two columns because a directory has exactly one owner and never needs to be queried by
     * kind alone — but the value is the same value, so a product uses one vocabulary for both.</p>
     */
    @Column(name = "owner_key", length = 64, nullable = false, updatable = false)
    private String ownerKey;

    /** Null at a root. The shape of the tree, and what the numbering is rebuilt from. */
    @Column(name = "parent_id", length = 36)
    private String parentId;

    @Column(name = "name", length = DirectoryPath.MAXIMUM_SEGMENT_LENGTH, nullable = false)
    private String name;

    @Column(name = "slug", length = DirectoryPath.MAXIMUM_SEGMENT_LENGTH, nullable = false)
    private String slug;

    /**
     * The whole path, from the root down, as {@code innoventa/files/manuals}.
     *
     * <p>⚠️ <strong>Denormalised on purpose, and maintained on every move.</strong> Reading a place
     * from a configuration file ({@code innoventa/files}) has to be one indexed lookup rather than a
     * walk, and the unique constraint on it is what stops two folders with the same name appearing in
     * one parent. The cost is that a move must rewrite this column for the whole subtree, which
     * {@code StorageDirectories} does in the same statement as the renumbering.</p>
     */
    @Column(name = "path", length = 1024, nullable = false)
    private String path;

    /** ⚠️ A root's name is part of every storage key beneath it, so a root may not move or rename. */
    @Column(name = "is_root", nullable = false)
    private boolean root;

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

    protected StorageDirectory() {
    }

    /**
     * 🏗️ A directory at a known place in the numbering.
     *
     * @param id        identifier the caller minted
     * @param ownerKey  whose tree, or {@link #INSTALLATION}
     * @param parentId  the directory above, or {@code null} at a root
     * @param name      what a person calls it
     * @param slug      what an address calls it
     * @param path      the whole path from the root down
     * @param root      whether this is a root
     * @param treeLeft  left bound of its range
     * @param treeRight right bound of its range
     * @param depth     how far down it sits, a root being one
     * @param sortOrder where it sits among its siblings
     */
    public StorageDirectory(String id, String ownerKey, String parentId, String name, String slug,
                            String path, boolean root, int treeLeft, int treeRight, int depth,
                            int sortOrder) {
        this.id        = id;
        this.ownerKey  = ownerKey;
        this.parentId  = parentId;
        this.name      = name;
        this.slug      = slug;
        this.path      = path;
        this.root      = root;
        this.treeLeft  = treeLeft;
        this.treeRight = treeRight;
        this.depth     = depth;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 🗄️ The storage namespace for anything filed here — the ROOT's path, never this one's.
     *
     * @return the namespace
     */
    public String namespace() {
        return DirectoryPath.of(path).namespace();
    }

    /**
     * 🌿 Whether a directory sits inside this one's range.
     *
     * @param other the candidate descendant
     * @return {@code true} when it is within, itself excluded
     */
    public boolean contains(StorageDirectory other) {
        return other.treeLeft > treeLeft && other.treeRight < treeRight;
    }

    public String getId() {
        return id;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isRoot() {
        return root;
    }

    public int getTreeLeft() {
        return treeLeft;
    }

    public void setTreeLeft(int treeLeft) {
        this.treeLeft = treeLeft;
    }

    public int getTreeRight() {
        return treeRight;
    }

    public void setTreeRight(int treeRight) {
        this.treeRight = treeRight;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StorageDirectory directory && id != null && id.equals(directory.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "StorageDirectory[%s]".formatted(path);
    }
}
