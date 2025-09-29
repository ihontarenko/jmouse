package org.jmouse.core.graph;

import org.jmouse.core.Visitor;

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
     *
     * @param graph the graph in which to find the path
     * @param start the starting node
     * @param end   the destination node
     * @return a list of nodes representing the shortest path from start to end, or an empty list
     * if no path exists
     */
    @Override
    public List<T> findPath(Graph<T> graph, T start, T end) {
        if (!graph.containsNode(start) || !graph.containsNode(end)) {
            return Collections.emptyList();
        }

        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        Queue<T>   queue  = new ArrayDeque<>();
        Visitor<T> seen   = new Visitor.Default<>();
        Map<T, T>  parent = new HashMap<>();

        seen.visit(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            T current = queue.poll();

            if (current.equals(end)) {
                break;
            }

            for (T neighbor : graph.getNeighbors(current)) {
                if (seen.unknown(neighbor)) {
                    seen.visit(neighbor);
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        if (!parent.containsKey(end)) {
            return Collections.emptyList();
        }

        List<T> path = new ArrayList<>();

        for (T current = end; current != null; current = parent.get(current)) {
            path.add(current);
        }

        Collections.reverse(path);

        return path;
    }

}
