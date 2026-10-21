package org.jmouse.tree.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jmouse.tree.NestedSetNode;
import org.jmouse.tree.NestedSetNumbering;
import org.jmouse.tree.NestedSetPlace;
import org.jmouse.tree.TreeException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 🌳 A nested set kept in a host's own table: the range reads it exists for, and the writes that keep its
 * numbering true.
 *
 * <p>The host keeps its entity, its table and its own rules — what a node is called, which names may stand
 * side by side, what a move rewrites besides the shape. This is the shape alone: where a node stands, what
 * is under it, what is over it.</p>
 *
 * <h3>⚠️ Every write renumbers, and the numbering is the point</h3>
 *
 * <p>A nested set makes "everything under here" a single indexed range, which is what an authorization
 * engine asks on every read of a subtree. The cost is paid on writes instead: inserting a node widens every
 * range to its right, and moving one renumbers its tree. Insertion is done in bulk statements rather than by
 * walking entities, because a tree of a few thousand nodes would otherwise load most of itself into memory to
 * add one.</p>
 *
 * <p>⚠️ <strong>Bulk statements bypass the persistence context.</strong> Anything already loaded still carries
 * the old numbering afterwards, so every write clears the context before returning, and returns a node read
 * anew. A caller holding a node from before a write holds stale numbers; skipping that is how a caller ends up
 * writing back numbers that were true a moment ago. Pending changes are flushed first, so a host that changed
 * its own columns before calling a write loses nothing.</p>
 *
 * <p>Transaction demarcation is the caller's, and a renumbering is only safe inside one.</p>
 */
public class NestedSetTree<N extends NestedSetNode> {

    private final EntityManager       entityManager;
    private final NestedSetMapping<N> mapping;

    /**
     * 🏗️ Work over the application's persistence context.
     *
     * @param entityManager the persistence context
     * @param mapping       which entity, which attribute separates its trees, how siblings are ordered
     */
    public NestedSetTree(EntityManager entityManager, NestedSetMapping<N> mapping) {
        this.entityManager = entityManager;
        this.mapping       = mapping;
    }

    // ── Reading ───────────────────────────────────────────────────────────────

    /** 🔎 One node by identifier, if it is there. */
    public Optional<N> find(String identifier) {
        return Optional.ofNullable(entityManager.find(mapping.type(), identifier));
    }

    /**
     * 🌿 Everything under a node, itself included, in tree order — one indexed range query, which is the whole
     * reason the numbering exists.
     *
     * @param node the node
     * @return the subtree, the node first
     */
    public List<N> subtreeOf(N node) {
        return query("WHERE node.%s = :scope AND node.treeLeft >= :left AND node.treeRight <= :right "
                     + "ORDER BY node.treeLeft", node)
            .getResultList();
    }

    /**
     * 🌿 Every node one sits inside, itself excluded — the whole chain, outermost first.
     *
     * <p>One indexed range query: it is asked on every authorized read beneath a node, so it cannot be a walk
     * up the parent pointers.</p>
     *
     * @param node the node
     * @return its ancestors, its root first
     */
    public List<N> ancestorsOf(N node) {
        return query("WHERE node.%s = :scope AND node.treeLeft < :left AND node.treeRight > :right "
                     + "ORDER BY node.treeLeft", node)
            .getResultList();
    }

    /**
     * 🌿 The nodes directly beneath one — one level, in tree order: what a tree that opens on demand asks for,
     * where {@link #subtreeOf} would send everything beneath before anybody opened anything.
     *
     * @param parent the node
     * @return its children
     */
    public List<N> childrenOf(N parent) {
        return entityManager.createQuery(
                select("WHERE node.%s = :scope AND node.parentId = :parentId ORDER BY node.treeLeft"
                           .formatted(mapping.scopeAttribute())), mapping.type())
            .setParameter("scope", scopeOf(parent))
            .setParameter("parentId", parent.getId())
            .getResultList();
    }

    /**
     * 🌱 The roots of one tree, in tree order.
     *
     * @param scope which tree
     * @return its roots
     */
    public List<N> roots(String scope) {
        return entityManager.createQuery(
                select("WHERE node.%s = :scope AND node.parentId IS NULL ORDER BY node.treeLeft"
                           .formatted(mapping.scopeAttribute())), mapping.type())
            .setParameter("scope", scope)
            .getResultList();
    }

    /**
     * 🌿 Everything under any of these, themselves included, each node once, in tree order.
     *
     * <p>⚠️ One query for the lot rather than one per node: a caller holding grants at a dozen places asks this
     * on every read, and a dozen round trips per request is how an authorization model becomes the reason a
     * product feels slow.</p>
     *
     * @param identifiers the nodes, of any trees of this table
     * @return the union of their subtrees
     */
    public List<N> subtreesOf(Collection<String> identifiers) {
        if (identifiers.isEmpty()) {
            return List.of();
        }

        return entityManager.createQuery(
                ("SELECT DISTINCT node FROM %1$s node, %1$s anchor WHERE anchor.id IN :identifiers "
                 + "AND node.%2$s = anchor.%2$s AND node.treeLeft >= anchor.treeLeft "
                 + "AND node.treeRight <= anchor.treeRight ORDER BY node.treeLeft")
                    .formatted(entityName(), mapping.scopeAttribute()), mapping.type())
            .setParameter("identifiers", identifiers)
            .getResultList();
    }

    /**
     * 📏 How many levels a node's subtree reaches below it: zero for a leaf.
     *
     * @param node the node
     * @return the height of its subtree
     */
    public int subtreeHeight(N node) {
        return subtreeOf(node).stream().mapToInt(NestedSetNode::getDepth).max().orElse(node.getDepth())
               - node.getDepth();
    }

    // ── Writing ───────────────────────────────────────────────────────────────

    /**
     * 🌱 A new root, numbered after the last node of its tree and ordered after its last root.
     *
     * @param scope which tree
     * @param build builds the host's node at the place made for it
     * @return the root, read anew
     */
    public N insertRoot(String scope, Function<NestedSetPlace, N> build) {
        int highest = highestBound(scope);

        return persisted(build.apply(new NestedSetPlace(null, highest + 1, highest + 2, 1, rootCount(scope))));
    }

    /**
     * 📁 A new node as the last child of another.
     *
     * @param parent where it goes
     * @param build  builds the host's node at the place made for it
     * @return the node, read anew
     */
    public N insertUnder(N parent, Function<NestedSetPlace, N> build) {
        String scope = scopeOf(parent);
        int    left  = parent.getTreeRight();

        // Make room: everything at or right of the parent's right bound shifts two places.
        shift(scope, "treeLeft", left, 2);
        shift(scope, "treeRight", left, 2);

        return persisted(build.apply(new NestedSetPlace(parent.getId(), left, left + 1, parent.getDepth() + 1,
                                                        nextSortOrder(parent.getId()))));
    }

    /**
     * 📦 Move a node, with everything under it, beneath another of the same tree.
     *
     * <p>Expressed as a new parent and executed as a renumbering: the pointer is the authority, so the
     * numbering is rebuilt from it rather than patched in place. What else a move changes in the host's row —
     * a path, a slug — the host changes on the node before calling, and it is flushed with the move.</p>
     *
     * @param node        the node to move
     * @param destination its new parent
     * @return the moved node, read anew
     * @throws TreeException when the destination is the node or beneath it, or in another tree
     */
    public N move(N node, N destination) {
        if (node.getId().equals(destination.getId()) || node.contains(destination)) {
            throw new TreeException("'%s' cannot be moved into itself or beneath itself".formatted(node.getId()));
        }

        // ⚠️ Never across trees. They are independent by construction — numbered apart —, so a move between
        // them would corrupt both.
        if (!scopeOf(node).equals(scopeOf(destination))) {
            throw new TreeException("'%s' and '%s' are in different trees and nothing moves between them"
                                        .formatted(node.getId(), destination.getId()));
        }

        node.setParentId(destination.getId());
        entityManager.flush();

        renumber(scopeOf(node));
        entityManager.clear();

        return require(node.getId());
    }

    /**
     * 🗑️ Remove a node and everything under it, and renumber its tree.
     *
     * <p>Whether a node with something under it may go at all is the host's question; this removes what it is
     * told to.</p>
     *
     * @param node the node
     */
    public void delete(N node) {
        String scope = scopeOf(node);

        for (N removed : subtreeOf(node)) {
            entityManager.remove(removed);
        }

        entityManager.flush();
        renumber(scope);
        entityManager.clear();
    }

    /**
     * 🔢 Rebuild one tree's numbering from its parent pointers ({@link NestedSetNumbering}) and write it.
     *
     * <p>⚠️ Scoped to one tree: rebuilding the whole table because somebody moved their own node would be a
     * write nobody expects, and the trees are independent by construction.</p>
     *
     * @param scope which tree
     */
    public void renumber(String scope) {
        List<N> all = entityManager.createQuery(
                select("WHERE node.%s = :scope".formatted(mapping.scopeAttribute())), mapping.type())
            .setParameter("scope", scope)
            .getResultList();

        NestedSetNumbering.number(all, mapping.siblingOrder());

        entityManager.flush();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private N persisted(N node) {
        entityManager.persist(node);
        entityManager.flush();
        entityManager.clear();

        return require(node.getId());
    }

    private N require(String identifier) {
        return find(identifier).orElseThrow(() -> new TreeException("No node '%s'".formatted(identifier)));
    }

    /** A read bounded by one node's tree and range. */
    private TypedQuery<N> query(String condition, N node) {
        return entityManager.createQuery(select(condition.formatted(mapping.scopeAttribute())), mapping.type())
            .setParameter("scope", scopeOf(node))
            .setParameter("left", node.getTreeLeft())
            .setParameter("right", node.getTreeRight());
    }

    private String select(String condition) {
        return "SELECT node FROM %s node %s".formatted(entityName(), condition);
    }

    /**
     * 🔢 Widen the numbering to the right of an insertion point — within ONE tree.
     *
     * <p>⚠️ Scoped by the tree, and it has to be: without that, one person adding a folder renumbers every other
     * person's tree in the table. Correctness would survive it, since the ranges stay contiguous either way, but
     * a write that touches every row is not a write anybody expects from adding one.</p>
     */
    private void shift(String scope, String bound, int from, int by) {
        entityManager.createQuery(("UPDATE %s node SET node.%s = node.%s + :by WHERE node.%s = :scope "
                                   + "AND node.%s >= :from")
                                      .formatted(entityName(), bound, bound, mapping.scopeAttribute(), bound))
            .setParameter("scope", scope)
            .setParameter("by", by)
            .setParameter("from", from)
            .executeUpdate();
    }

    private int highestBound(String scope) {
        Integer highest = entityManager.createQuery(
                "SELECT MAX(node.treeRight) FROM %s node WHERE node.%s = :scope"
                    .formatted(entityName(), mapping.scopeAttribute()), Integer.class)
            .setParameter("scope", scope)
            .getSingleResult();

        return highest == null ? 0 : highest;
    }

    private int rootCount(String scope) {
        return entityManager.createQuery(
                "SELECT COUNT(node) FROM %s node WHERE node.%s = :scope AND node.parentId IS NULL"
                    .formatted(entityName(), mapping.scopeAttribute()), Long.class)
            .setParameter("scope", scope)
            .getSingleResult()
            .intValue();
    }

    private int nextSortOrder(String parentId) {
        Integer highest = entityManager.createQuery(
                "SELECT MAX(node.sortOrder) FROM %s node WHERE node.parentId = :parentId".formatted(entityName()),
                Integer.class)
            .setParameter("parentId", parentId)
            .getSingleResult();

        return highest == null ? 0 : highest + 1;
    }

    private String scopeOf(N node) {
        return mapping.scopeOf().apply(node);
    }

    /** The entity's name in a query: its own, or its class's simple name when it declares none. */
    private String entityName() {
        return entityManager.getMetamodel().entity(mapping.type()).getName();
    }
}
