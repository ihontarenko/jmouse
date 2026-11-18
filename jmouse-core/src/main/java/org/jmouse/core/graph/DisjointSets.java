package org.jmouse.core.graph;

import java.util.Arrays;

/**
 * Elements joined into sets, each set named by one of its elements — its root —: copper nodes that conduct as one, track ends that
 * meet at one point. An element is added as a set of its own, and two sets joined become one; asking an element's root shortens the
 * way to it for every later ask. Elements are numbered as they are added, from 0.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class DisjointSets {

    private int[] parents;
    private int   size;

    /** No elements yet: each is added as it comes. */
    public DisjointSets() {
        this(0);
    }

    /** {@code size} elements, each a set of its own. */
    public DisjointSets(int size) {
        this.parents = new int[Math.max(size, 1)];
        for (int element = 0; element < size; element++) {
            parents[element] = element;
        }
        this.size = size;
    }

    /** An element added as a set of its own: its number. */
    public int add() {
        if (size == parents.length) {
            parents = Arrays.copyOf(parents, parents.length * 2);
        }
        parents[size] = size;
        return size++;
    }

    /** How many elements there are. */
    public int size() {
        return size;
    }

    /** The element that names the set an element is in; the way to it halved on the way. */
    public int rootOf(int element) {
        int root = element;
        while (parents[root] != root) {
            parents[root] = parents[parents[root]];
            root = parents[root];
        }
        return root;
    }

    /** The sets two elements are in, one set; named by the second's root. */
    public void join(int one, int other) {
        parents[rootOf(one)] = rootOf(other);
    }

    /** Whether two elements are in one set. */
    public boolean together(int one, int other) {
        return rootOf(one) == rootOf(other);
    }
}
