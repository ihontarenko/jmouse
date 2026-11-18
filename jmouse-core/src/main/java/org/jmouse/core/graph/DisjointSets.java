package org.jmouse.core.graph;

import java.util.Arrays;

/**
 * Elements joined into sets, each set named by its smallest element — its root —, so a set is named the same however its joins
 * came: copper nodes that conduct as one, track ends that meet at one point, pieces of copper that are one. An element is added as
 * a set of its own, and two sets joined become one; asking an element's root shortens the way to it for every later ask. Elements are numbered as they are added, from 0.
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

    /** The sets two elements are in, one set; named by the smaller of their roots. */
    public void join(int one, int other) {
        int first = rootOf(one);
        int second = rootOf(other);
        parents[Math.max(first, second)] = Math.min(first, second);
    }

    /** Every element's root, worked out once: by element. */
    public int[] roots() {
        int[] roots = new int[size];
        for (int element = 0; element < size; element++) {
            roots[element] = rootOf(element);
        }
        return roots;
    }

    /** Whether two elements are in one set. */
    public boolean together(int one, int other) {
        return rootOf(one) == rootOf(other);
    }
}
