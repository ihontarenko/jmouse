package org.jmouse.tree;

/**
 * 🌳 One node of a hierarchy kept as a nested set: a parent pointer, and a numbered range beside it.
 *
 * <h3>⚠️ Two descriptions of one tree, and the pointer is the authority</h3>
 *
 * <p>{@link #getParentId()} says where a node hangs. {@link #getTreeLeft()} and {@link #getTreeRight()} say
 * the same thing a second time, as a range that holds the ranges of everything beneath it — which is
 * what makes "everything under this node" one indexed range query instead of a recursive descent, and an
 * authorization engine granting a subtree asks exactly that on every read. The numbering is derived: a
 * move is <em>expressed</em> as a new parent and <em>executed</em> as a renumbering from the pointers
 * ({@link NestedSetNumbering}), so a numbering that ever drifts can always be rebuilt.</p>
 *
 * <p>A root has no parent. Its depth is one, and every level below adds one.</p>
 *
 * <p>The names are getters and setters on purpose: the host's entity is a persistence entity, these are
 * its attributes, and a query over its table names them the same way.</p>
 */
public interface NestedSetNode {

    /** What identifies the node within its host's table. */
    String getId();

    /** The node above, or {@code null} at a root. */
    String getParentId();

    void setParentId(String parentId);

    /** The left bound of the node's range: everything beneath it is numbered strictly inside. */
    int getTreeLeft();

    void setTreeLeft(int treeLeft);

    /** The right bound of the node's range. */
    int getTreeRight();

    void setTreeRight(int treeRight);

    /** How far down the node sits, a root being one. */
    int getDepth();

    void setDepth(int depth);

    /** Where the node stands among its siblings: the first of the comparisons that order them. */
    int getSortOrder();

    /** Whether the node has no parent. */
    default boolean isTreeRoot() {
        return getParentId() == null;
    }

    /**
     * 🌿 Whether another node sits inside this one's range.
     *
     * @param other the candidate descendant, of the same tree
     * @return {@code true} when it is within, this node itself excluded
     */
    default boolean contains(NestedSetNode other) {
        return other.getTreeLeft() > getTreeLeft() && other.getTreeRight() < getTreeRight();
    }
}
