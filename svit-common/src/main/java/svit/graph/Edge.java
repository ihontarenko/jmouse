package svit.graph;

/**
 * Represents an undirected edge connecting two nodes of type {@code T} in a graph.
 * This class encapsulates a connection between {@link #nodeA} and {@link #nodeB}.
 *
 * @param <T> the type of the nodes connected by this edge
 */
public record Edge<T>(T nodeA, T nodeB) {

    @Override
    public String toString() {
        return "[%s:%s]".formatted(nodeA, nodeB);
    }

}
