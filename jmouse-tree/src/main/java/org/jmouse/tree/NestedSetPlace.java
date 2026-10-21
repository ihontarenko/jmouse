package org.jmouse.tree;

/**
 * 📍 Where a new node goes: the room the tree made for it, handed to the host that builds the node.
 *
 * <p>The tree decides the numbers; the host decides everything else a node of its own is — its name, its
 * identifier, what else its row carries — and builds it here.</p>
 *
 * @param parentId  the node above, or {@code null} for a root
 * @param treeLeft  the left bound made free for it
 * @param treeRight the right bound, {@code treeLeft + 1}: a new node holds nothing yet
 * @param depth     one below its parent, or one for a root
 * @param sortOrder after its last sibling
 */
public record NestedSetPlace(String parentId, int treeLeft, int treeRight, int depth, int sortOrder) {
}
