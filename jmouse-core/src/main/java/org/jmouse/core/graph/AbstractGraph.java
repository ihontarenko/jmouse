package org.jmouse.core.graph;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🧩 Base class for {@link Graph} implementations backed by a concurrent adjacency map.
 *
 * <p>Each node maps to a concurrent neighbor {@link Set} stored in a {@link ConcurrentHashMap}.
 * Subclasses define edge semantics (directed/undirected) and multi-step operations (e.g., add/remove edge).</p>
 *
 * <h3>Concurrency</h3>
 * <ul>
 *   <li>Neighbor sets are created via {@link ConcurrentHashMap#newKeySet()} for safe concurrent updates.</li>
 *   <li>Returned sets have different mutability guarantees:
 *     <ul>
 *       <li>{@link #addNode(Object)} → returns a <em>live</em> concurrent set (modifiable view).</li>
 *       <li>{@link #getNeighbors(Object)} → returns an <em>immutable snapshot</em> ({@code Set.copyOf}).</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 *   <li>❌ {@code null} nodes are not supported (per {@link ConcurrentHashMap} constraints).</li>
 *   <li>🧭 Edge directionality and higher-level invariants are the responsibility of subclasses.</li>
 * </ul>
 *
 * @param <T> node type
 */
public abstract class AbstractGraph<T> implements Graph<T> {

    /** 📦 Adjacency map: node → concurrent set of neighbors. */
    protected final Map<T, Set<T>> adjacency = new ConcurrentHashMap<>();

    /**
     * 🧩 Ensures that {@code node} exists and returns its <em>live</em> neighbor set.
     *
     * <p>If the node is absent, a new concurrent set is created and stored.</p>
     *
     * @param node the node to ensure
     * @return the live concurrent neighbor set for {@code node} (never {@code null})
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public Set<T> addNode(T node) {
        return adjacency.computeIfAbsent(node, k -> ConcurrentHashMap.newKeySet());
    }

    /**
     * 🔍 Retrieves an <em>immutable snapshot</em> of neighbors for {@code node}.
     *
     * <p>If the node is missing, returns an empty immutable set.</p>
     *
     * @param node the node whose neighbors are requested
     * @return an immutable set of neighboring nodes (possibly empty, never {@code null})
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public Set<T> getNeighbors(T node) {
        return Set.copyOf(adjacency.getOrDefault(node, Set.of()));
    }

    /**
     * ✅ Checks whether {@code node} exists in the graph.
     *
     * @param node the node to check
     * @return {@code true} if present; {@code false} otherwise
     * @throws NullPointerException if {@code node} is {@code null}
     */
    @Override
    public boolean containsNode(T node) {
        return adjacency.containsKey(node);
    }
}
