package svit.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe implementation of a directed graph using a map of lists to represent
 * adjacency relationships between nodes of type {@code T}. This graph stores neighbors
 * for each node in a {@link ConcurrentHashMap} to allow concurrent modifications.
 *
 * <p>Edges are added in both directions, effectively making this an undirected graph.
 * However, the interface used (`Graph<T>`) implies directionality and can be adapted
 * for directed behaviors if needed.
 *
 * @param <T> the type of nodes in the graph
 */
public class MapListGraph<T> implements Graph<T> {

    private final Map<T, List<T>> adjacency = new ConcurrentHashMap<>();

    /**
     * Adds an undirected edge between the {@code from} and {@code to} nodes.
     * If the nodes do not exist in the graph, they will be added. The edge is
     * added in both directions: {@code from -> to} and {@code to -> from}.
     *
     * @param from the starting node of the edge
     * @param to   the ending node of the edge
     */
    @Override
    public void addEdge(T from, T to) {
        addNode(from).add(to);
        addNode(to);
    }

    /**
     * Adds a node to the graph if it does not already exist.
     * If the node is new, an empty list of neighbors is created and stored.
     * Otherwise, returns the existing list of neighbors.
     *
     * @param node the node to add to the graph
     * @return the list of neighbors associated with the node
     */
    @Override
    public List<T> addNode(T node) {
        return adjacency.computeIfAbsent(node, n -> new ArrayList<>());
    }

    /**
     * Retrieves the list of neighbors for the specified node.
     * If the node is not present in the graph, returns an empty list.
     *
     * @param node the node whose neighbors are to be retrieved
     * @return a list of neighboring nodes, or an empty list if the node has no neighbors or does not exist
     */
    @Override
    public List<T> getNeighbors(T node) {
        return adjacency.getOrDefault(node, List.of());
    }

    /**
     * Checks whether the specified node exists in the graph.
     *
     * @param node the node to check for existence
     * @return {@code true} if the node exists in the graph, {@code false} otherwise
     */
    @Override
    public boolean containsNode(T node) {
        return adjacency.containsKey(node);
    }
}
