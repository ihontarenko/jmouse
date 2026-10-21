package org.jmouse.tree.jpa;

import org.jmouse.tree.NestedSetNode;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

/**
 * 🗺️ How a host's entity is a tree: which entity, which attribute separates one tree from the next, and the
 * order siblings stand in.
 *
 * <p>⚠️ The node attributes are named as {@link NestedSetNode} names them — {@code id}, {@code parentId},
 * {@code treeLeft}, {@code treeRight}, {@code depth}, {@code sortOrder} — and queries over the host's table
 * use those names. The columns behind them are the host's to choose.</p>
 *
 * <h3>⚠️ The scope is what makes the trees independent</h3>
 *
 * <p>One table holds many trees — one per owner, per taxonomy —, numbered apart. Every read and every write
 * is bounded by the scope: without it, one person adding a folder renumbers every other person's tree, and a
 * range read could answer with somebody else's nodes, since two trees number from one alike.</p>
 *
 * @param type           the host's entity
 * @param scopeAttribute the attribute that names the tree a node belongs to — {@code ownerKey}
 * @param scopeOf        the same value read from a node
 * @param siblingOrder   the order of the children of one parent, and of the roots, when the tree is renumbered
 */
public record NestedSetMapping<N extends NestedSetNode>(Class<N> type, String scopeAttribute,
                                                        Function<? super N, String> scopeOf,
                                                        Comparator<? super N> siblingOrder) {

    public NestedSetMapping {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(scopeAttribute, "scopeAttribute");
        Objects.requireNonNull(scopeOf, "scopeOf");
        Objects.requireNonNull(siblingOrder, "siblingOrder");
    }
}
