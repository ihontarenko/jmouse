package org.jmouse.core.graph;

import java.util.List;

/**
 * An interface defining the contract for finding paths within a graph.
 * Implementations of this interface should provide algorithms to determine
 * a path from a start node to an end node within a given graph.
 *
 * @param <T> the type of nodes in the graph
 */
public interface PathFinder<T> {

    /**
     * Finds a path from the {@code start} node to the {@code end} node within the specified graph.
     *
     * <p>The method should return a list of nodes representing the path from {@code start}
     * to {@code end}, inclusive. If no such path exists, it should return an empty list.
     *
     * <p>Implementations may use various graph traversal algorithms such as Breadth-First Search (BFS),
     * Depth-First Search (DFS), Dijkstra's algorithm, A* search, etc., depending on the requirements.
     *
     * @param graph the graph in which to find the path
     * @param start the starting node from which the path should begin
     * @param end the destination node at which the path should end
     * @return a {@link List} of nodes representing the path from {@code start} to {@code end},
     *         or an empty list if no path exists between them
     * @throws IllegalArgumentException if {@code graph} is {@code null}, or if {@code start}
     *                                  or {@code end} nodes are not present in the graph
     */
    List<T> findPath(Graph<T> graph, T start, T end);
}
