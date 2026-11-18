package org.jmouse.geometry.index;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Boxes in a tree packed once, bottom up, by sorting and tiling (STR — sort, tile, recurse): the boxes sorted by their middles across,
 * cut into upright slices, each slice sorted down and cut into nodes of a few boxes; the nodes so made packed the same way, level by
 * level, to one root. What comes near a box is found by walking only the nodes whose bounds reach it. The index for items scattered over
 * a wide field, where a grid would be mostly empty cells.
 * <p>
 * Laid out flat: every node's bounds in four arrays of doubles, and its children — boxes' numbers for a leaf, nodes' for the rest — a
 * run of one array of ints.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class BoxTree implements BoxIndex {

    /** How many children a node takes where none is asked: few enough that a node's walk is short, enough that the tree is shallow. */
    public static final int DEFAULT_NODE_SIZE = 10;

    private final int      count;
    private final double[] lefts;
    private final double[] tops;
    private final double[] rights;
    private final double[] bottoms;
    /** Where each node's children start in {@link #children}; a node's run ends where the next node's starts. */
    private final int[]    starts;
    private final int[]    children;
    /** The nodes numbered below this are leaves, their children boxes. */
    private final int      leaves;
    private final int      root;
    private final List<Box> boxes;

    private BoxTree(List<Box> boxes, int nodeSize) {
        if (nodeSize < 2) {
            throw new IllegalArgumentException("A node takes at least two children, not " + nodeSize);
        }
        this.boxes = List.copyOf(boxes);
        this.count = boxes.size();
        Levels levels = new Levels(boxes, nodeSize);
        this.lefts = levels.lefts;
        this.tops = levels.tops;
        this.rights = levels.rights;
        this.bottoms = levels.bottoms;
        this.starts = levels.starts;
        this.children = levels.children;
        this.leaves = levels.leaves;
        this.root = levels.nodes - 1;
    }

    /** The boxes, each known by its place in the list, in nodes of {@link #DEFAULT_NODE_SIZE}. */
    public static BoxTree of(List<Box> boxes) {
        return new BoxTree(boxes, DEFAULT_NODE_SIZE);
    }

    /** The boxes, each known by its place in the list, in nodes of as many children as asked. */
    public static BoxTree of(List<Box> boxes, int nodeSize) {
        return new BoxTree(boxes, nodeSize);
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public int[] near(Box box) {
        if (count == 0) {
            return new int[0];
        }
        int[] found = new int[16];
        int size = 0;
        int[] waiting = new int[64];
        int depth = 0;
        waiting[depth++] = root;
        while (depth > 0) {
            int node = waiting[--depth];
            if (!reaches(node, box)) {
                continue;
            }
            int end = starts[node + 1];
            for (int at = starts[node]; at < end; at++) {
                int child = children[at];
                if (node < leaves) {
                    if (boxes.get(child).within(box, 0)) {
                        if (size == found.length) {
                            found = Arrays.copyOf(found, size * 2);
                        }
                        found[size++] = child;
                    }
                } else {
                    if (depth == waiting.length) {
                        waiting = Arrays.copyOf(waiting, depth * 2);
                    }
                    waiting[depth++] = child;
                }
            }
        }
        int[] numbers = Arrays.copyOf(found, size);
        Arrays.sort(numbers);
        return numbers;
    }

    @Override
    public BitSet nearSet(Box box) {
        BitSet found = new BitSet(count);
        for (int number : near(box)) {
            found.set(number);
        }
        return found;
    }

    private boolean reaches(int node, Box box) {
        return lefts[node] <= box.right() && box.left() <= rights[node] && tops[node] <= box.bottom() && box.top() <= bottoms[node];
    }

    /** The tree's levels packed bottom up, into growing flat arrays: the leaves first, the root last. */
    private static final class Levels {

        private double[] lefts    = new double[16];
        private double[] tops     = new double[16];
        private double[] rights   = new double[16];
        private double[] bottoms  = new double[16];
        private int[]    starts   = new int[17];
        private int[]    children = new int[16];
        private int      nodes;
        private int      written;
        private int      leaves;

        Levels(List<Box> boxes, int nodeSize) {
            int[] level = new int[boxes.size()];
            for (int number = 0; number < level.length; number++) {
                level[number] = number;
            }
            double[][] bounds = new double[4][];
            bounds[0] = boxes.stream().mapToDouble(Box::left).toArray();
            bounds[1] = boxes.stream().mapToDouble(Box::top).toArray();
            bounds[2] = boxes.stream().mapToDouble(Box::right).toArray();
            bounds[3] = boxes.stream().mapToDouble(Box::bottom).toArray();
            boolean ofBoxes = true;
            do {
                int firstMade = nodes;
                packed(level, bounds, nodeSize, ofBoxes);
                if (ofBoxes) {
                    leaves = nodes;
                }
                level = new int[nodes - firstMade];
                for (int at = 0; at < level.length; at++) {
                    level[at] = firstMade + at;
                }
                bounds = new double[][] {lefts, tops, rights, bottoms};
                ofBoxes = false;
            } while (level.length > 1);
            starts = Arrays.copyOf(starts, nodes + 1);
            starts[nodes] = written;
        }

        /** One level's entries — boxes or nodes, by their bounds — sorted and tiled into nodes of the next level up. */
        private void packed(int[] entries, double[][] bounds, int nodeSize, boolean ofBoxes) {
            if (entries.length == 0) {
                made(entries, 0, 0, bounds);
                return;
            }
            Integer[] sorted = Arrays.stream(entries).boxed().toArray(Integer[]::new);
            Arrays.sort(sorted, (one, other) -> Double.compare(bounds[0][one] + bounds[2][one], bounds[0][other] + bounds[2][other]));
            int nodesNeeded = (entries.length + nodeSize - 1) / nodeSize;
            int slices = (int) Math.ceil(Math.sqrt(nodesNeeded));
            int perSlice = slices * nodeSize;
            for (int from = 0; from < sorted.length; from += perSlice) {
                int to = Math.min(from + perSlice, sorted.length);
                Arrays.sort(sorted, from, to, (one, other) -> Double.compare(bounds[1][one] + bounds[3][one], bounds[1][other] + bounds[3][other]));
                for (int nodeFrom = from; nodeFrom < to; nodeFrom += nodeSize) {
                    int[] run = new int[Math.min(nodeSize, to - nodeFrom)];
                    for (int at = 0; at < run.length; at++) {
                        run[at] = sorted[nodeFrom + at];
                    }
                    made(run, 0, run.length, bounds);
                }
            }
        }

        /** A node of these children, its bounds the box holding theirs. */
        private void made(int[] run, int from, int to, double[][] bounds) {
            if (nodes + 1 >= lefts.length) {
                int grown = lefts.length * 2;
                lefts = Arrays.copyOf(lefts, grown);
                tops = Arrays.copyOf(tops, grown);
                rights = Arrays.copyOf(rights, grown);
                bottoms = Arrays.copyOf(bottoms, grown);
                starts = Arrays.copyOf(starts, grown + 1);
            }
            double left = Double.POSITIVE_INFINITY;
            double top = Double.POSITIVE_INFINITY;
            double right = Double.NEGATIVE_INFINITY;
            double bottom = Double.NEGATIVE_INFINITY;
            starts[nodes] = written;
            for (int at = from; at < to; at++) {
                int child = run[at];
                left = Math.min(left, bounds[0][child]);
                top = Math.min(top, bounds[1][child]);
                right = Math.max(right, bounds[2][child]);
                bottom = Math.max(bottom, bounds[3][child]);
                if (written == children.length) {
                    children = Arrays.copyOf(children, written * 2);
                }
                children[written++] = child;
            }
            lefts[nodes] = left;
            tops[nodes] = top;
            rights[nodes] = right;
            bottoms[nodes] = bottom;
            nodes++;
        }
    }
}
