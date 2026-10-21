package org.jmouse.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 🔢 A tree's whole numbering, rebuilt from its parent pointers.
 *
 * <p>⚠️ <strong>Rebuilt rather than patched, and that is the safety property.</strong> Patching a move means
 * four range updates that must all be right; getting one wrong leaves a numbering that looks plausible and
 * answers subtree questions incorrectly — which, where a subtree is what access is granted on, is a silent
 * authorization bug. Rebuilding from the pointers cannot drift, and the pointers are what a person edits.</p>
 *
 * <p>Roots are numbered one after another, each subtree depth first, siblings in the order the host gives.
 * Left bounds start at one; a root's depth is one.</p>
 */
public final class NestedSetNumbering {

    private NestedSetNumbering() {
    }

    /**
     * 🔢 Number every node of one tree.
     *
     * @param nodes        every node of the tree — a whole tree, never a part of one
     * @param siblingOrder the order of the children of one parent, and of the roots
     * @throws TreeException when a node's parent is not among the nodes, or the pointers go round in a
     *                       circle: either would leave a node unnumbered, answering range queries wrongly
     */
    public static <N extends NestedSetNode> void number(Collection<N> nodes, Comparator<? super N> siblingOrder) {
        Map<String, List<N>> childrenByParent = new HashMap<>();
        List<N>              roots            = new ArrayList<>();

        for (N node : nodes) {
            if (node.isTreeRoot()) {
                roots.add(node);
            } else {
                childrenByParent.computeIfAbsent(node.getParentId(), ignored -> new ArrayList<>()).add(node);
            }
        }

        roots.sort(siblingOrder);
        childrenByParent.values().forEach(children -> children.sort(siblingOrder));

        Set<String> numbered = new HashSet<>();
        int         counter  = 0;

        for (N root : roots) {
            counter = number(root, childrenByParent, counter + 1, 1, numbered);
        }

        if (numbered.size() != nodes.size()) {
            List<String> unreached = nodes.stream().map(NestedSetNode::getId)
                    .filter(identifier -> !numbered.contains(identifier)).toList();
            throw new TreeException(("%d node(s) hang from no root — a parent that is not in the tree, or pointers "
                    + "that go round in a circle: %s").formatted(unreached.size(), unreached));
        }
    }

    private static <N extends NestedSetNode> int number(N node, Map<String, List<N>> childrenByParent, int left,
                                                        int depth, Set<String> numbered) {
        if (!numbered.add(node.getId())) {
            throw new TreeException("'%s' is reached twice: its pointers go round in a circle".formatted(node.getId()));
        }

        int counter = left;

        node.setTreeLeft(left);
        node.setDepth(depth);

        for (N child : childrenByParent.getOrDefault(node.getId(), List.of())) {
            counter = number(child, childrenByParent, counter + 1, depth + 1, numbered);
        }

        node.setTreeRight(counter + 1);

        return counter + 1;
    }
}
