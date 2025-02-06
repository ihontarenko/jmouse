package org.jmouse.core.graph;

import org.jmouse.util.Visitor;

import java.util.*;

/**
 * An implementation of the {@link PathFinder} interface using Breadth-First Search (BFS).
 * This class attempts to find the shortest path between two nodes in a graph.
 *
 * @param <T> the type of nodes in the graph
 */
public class BFSPathFinder<T> implements PathFinder<T> {

    /**
     * Finds the shortest path between the start and end nodes in the given graph using BFS.

     * @param graph the graph in which to find the path
     * @param start the starting node
     * @param end the destination node
     * @return a list of nodes representing the shortest path from start to end, or an empty list
     *         if no path exists
     */
    @Override
    public List<T> findPath(Graph<T> graph, T start, T end) {
        List<T> path = Collections.emptyList();

        if (graph.containsNode(start) && graph.containsNode(end)) {
            Queue<List<T>> queue   = new LinkedList<>();
            Visitor<T>     visitor = new Visitor.Default<>();

            visitor.visit(start);
            queue.add(List.of(start));

            while (!queue.isEmpty()) {
                List<T> current = queue.poll();
                T       last    = current.getLast();

                if (last.equals(end)) {
                    path = current;
                    break;
                }

                for (T neighbor : graph.getNeighbors(last)) {
                    if (visitor.unknown(neighbor)) {
                        visitor.visit(neighbor);

                        List<T> newPath = new ArrayList<>(current);
                        newPath.add(neighbor);

                        queue.add(newPath);
                    }
                }
            }
        }

        return path;
    }

}
