package org.jmouse.core.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic interface representing a directed graph structure where nodes of type {@code T}
 * can be connected by edges. This interface provides basic operations to add nodes and edges,
 * retrieve neighbors of a node, and check for the existence of a node in the graph.
 *
 * @param <T> the type of nodes in the graph
 */
public interface Graph<T> {

    /**
     * Adds a directed edge from one node to another in the graph. If either node does not
     * exist in the graph, the behavior depends on the implementation (it might add the missing nodes
     * automatically or require them to be added beforehand).
     *
     * @param from the source node of the edge
     * @param to   the destination node of the edge
     */
    void addEdge(T from, T to);

    /**
     * Adds a new node to the graph. If the node already exists, the behavior depends on the
     * implementation (it might ignore the addition or update the existing node).
     *
     * @param node the node to add to the graph
     * @return a list containing the added node(s); depending on the implementation, this could
     *         return the existing node or a list of nodes that were added as a result of this operation
     */
    List<T> addNode(T node);

    /**
     * Retrieves a list of neighboring nodes directly connected from the specified node.
     * In a directed graph, these are the nodes that can be reached by outgoing edges from the given node.
     *
     * @param node the node whose neighbors are to be retrieved
     * @return a {@link List} of neighboring nodes; if the node has no neighbors, returns an empty list
     * @throws IllegalArgumentException if the node does not exist in the graph
     */
    List<T> getNeighbors(T node);

    /**
     * Checks if the graph contains the specified node.
     *
     * @param node the node to check for existence in the graph
     * @return {@code true} if the node exists in the graph, {@code false} otherwise
     */
    boolean containsNode(T node);

    /**
     * Utility method to convert a list of nodes into a list of consecutive edges connecting those nodes.
     *
     * <p>This method takes a list of nodes and constructs edges between each pair of consecutive
     * nodes in the list. For example, given a list [A, B, C, D], it will produce edges:
     * (A, B), (B, C), (C, D).</p>
     *
     * @param <T> the type of the nodes in the list and edges
     * @param nodes the list of nodes to be converted into edges
     * @return a list of {@link Edge} objects connecting consecutive nodes
     */
    static <T> List<Edge<T>> toEdges(List<T> nodes) {
        List<Edge<T>> edges = new ArrayList<>();
        int           n     = nodes.size() - 1;

        for (int i = 0; i < n; i++) {
            edges.add(new Edge<>(nodes.get(i), nodes.get(i + 1)));
        }

        return edges;
    }
}
