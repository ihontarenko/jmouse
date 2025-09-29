package org.jmouse.core.graph;

/**
 * 🔁 Undirected graph backed by {@link AbstractGraph}'s concurrent adjacency map.
 *
 * <p>Edges are <b>symmetric</b>: invoking {@link #addEdge(Object, Object)} links nodes both ways
 * ({@code a ↔ b}). The neighbor set for a node represents all adjacent nodes in the undirected sense.</p>
 *
 * <h3>Behavior</h3>
 * <ul>
 *   <li>Set semantics: duplicate edges are ignored; self-loops ({@code a ↔ a}) are allowed.</li>
 *   <li>Concurrency &amp; mutability follow {@link AbstractGraph}:
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
public class UndirectedMapGraph<T> extends AbstractGraph<T> {

    /**
     * ➕ Adds an undirected edge between {@code a} and {@code b}.
     *
     * <p>Ensures both nodes exist, then inserts {@code b} into {@code a}'s neighbor set
     * and {@code a} into {@code b}'s neighbor set.</p>
     *
     * @param a one endpoint
     * @param b the other endpoint
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    @Override
    public void addEdge(T a, T b) {
        addNode(a).add(b);
        addNode(b).add(a);
    }
}
