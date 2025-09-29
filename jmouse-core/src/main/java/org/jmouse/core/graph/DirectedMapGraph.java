package org.jmouse.core.graph;

/**
 * ➡️ Directed graph backed by {@link AbstractGraph}'s concurrent adjacency map.
 *
 * <p>Edges are <b>one-way</b>: calling {@link #addEdge(Object, Object)} creates an
 * outgoing edge {@code a → b}. Node {@code b} is ensured to exist but is not linked
 * back to {@code a}.</p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Neighbors returned by {@link #getNeighbors(Object)} are the <em>outgoing</em> neighbors.</li>
 *   <li>Concurrency characteristics and mutability rules are inherited from {@link AbstractGraph}:
 *     <ul>
 *       <li>{@code addNode(...)} returns a <em>live</em> concurrent set.</li>
 *       <li>{@code getNeighbors(...)} returns an <em>immutable snapshot</em>.</li>
 *     </ul>
 *   </li>
 *   <li>❌ {@code null} nodes are not supported.</li>
 * </ul>
 *
 * @param <T> node type
 */
public class DirectedMapGraph<T> extends AbstractGraph<T> {

    /**
     * ➕ Adds a directed edge {@code a → b}.
     *
     * <p>Ensures both nodes exist. Inserts {@code b} into {@code a}'s outgoing
     * neighbor set; does <b>not</b> add the reverse link.</p>
     *
     * @param a source node
     * @param b target node
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    @Override
    public void addEdge(T a, T b) {
        addNode(a).add(b);
        addNode(b);
    }
}
