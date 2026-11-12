package org.jmouse.cad.document.search;

import java.util.Arrays;

/**
 * The search every router runs — a sheet's wires, a board's tracks: A* over states numbered from 0, each a point of a lattice
 * and what else the router tells apart there (the direction it was reached in, the layer it is on). The router says what a
 * state costs to reach and how far it still is from the goal; this keeps, per state, the cheapest cost known and where it came
 * from, and the open states cheapest first.
 *
 * <p>Its rows are kept from one search to the next and grown only when a search needs more: a search marks what it wrote with
 * its own generation instead of clearing them, so a board's every track tried again and again allocates and clears nothing per
 * search. A state opened again more cheaply leaves its dearer opening in the queue; that one is passed over when it comes up,
 * never expanded twice. One search runs at a time: a router that searches on several threads holds one of these for each.</p>
 */
public final class LatticeSearch {

    /** How many open states the queue holds before it first grows. */
    private static final int FIRST_QUEUE = 1024;

    private int      generation;
    private int[]    stamp       = new int[0];
    private double[] best        = new double[0];
    private int[]    parents     = new int[0];
    private double[] queueScore  = new double[FIRST_QUEUE];
    private double[] queueCost   = new double[FIRST_QUEUE];
    private int[]    queueState  = new int[FIRST_QUEUE];
    private int      queueSize;

    /** Ready for a search over {@code states} states: every state unreached, nothing open. */
    public void begin(int states) {
        if (stamp.length < states) {
            stamp = new int[states];
            best = new double[states];
            parents = new int[states];
            generation = 0;
        }
        generation += 1;
        if (generation == Integer.MAX_VALUE) {
            Arrays.fill(stamp, 0);
            generation = 1;
        }
        queueSize = 0;
    }

    /** The cheapest cost a state is known to be reached at; infinity while unreached. */
    public double costOf(int state) {
        return stamp[state] == generation ? best[state] : Double.POSITIVE_INFINITY;
    }

    /** Where a state was reached from on its cheapest way; -1 for a start. */
    public int parentOf(int state) {
        return parents[state];
    }

    /**
     * A state reached at {@code cost} from {@code parent} (-1 for a start): kept and opened, ordered by {@code score} — its cost
     * and the estimate of what is left —, when cheaper than any way to it known; else nothing. Whether it was kept.
     */
    public boolean offer(int state, double cost, int parent, double score) {
        if (cost >= costOf(state)) {
            return false;
        }
        stamp[state] = generation;
        best[state] = cost;
        parents[state] = parent;
        push(score, cost, state);
        return true;
    }

    /** The open state of the lowest score worth expanding; -1 when none is left. */
    public int next() {
        while (queueSize > 0) {
            int state = queueState[0];
            double cost = queueCost[0];
            removeTop();
            if (cost <= costOf(state)) {
                return state;
            }
        }
        return -1;
    }

    /** The states of the cheapest way to {@code state}, from its start. */
    public int[] wayTo(int state) {
        int length = 0;
        for (int at = state; at >= 0; at = parents[at]) {
            length++;
        }
        int[] way = new int[length];
        int place = length - 1;
        for (int at = state; at >= 0; at = parents[at]) {
            way[place--] = at;
        }
        return way;
    }

    private void push(double score, double cost, int state) {
        if (queueSize == queueScore.length) {
            grow();
        }
        int index = queueSize++;
        while (index > 0) {
            int parent = (index - 1) >> 1;
            if (queueScore[parent] <= score) {
                break;
            }
            move(parent, index);
            index = parent;
        }
        place(index, score, cost, state);
    }

    private void removeTop() {
        int last = --queueSize;
        if (last <= 0) {
            return;
        }
        double score = queueScore[last];
        double cost = queueCost[last];
        int state = queueState[last];
        int index = 0;
        while (index * 2 + 1 < last) {
            int child = index * 2 + 1;
            if (child + 1 < last && queueScore[child + 1] < queueScore[child]) {
                child++;
            }
            if (queueScore[child] >= score) {
                break;
            }
            move(child, index);
            index = child;
        }
        place(index, score, cost, state);
    }

    private void move(int from, int to) {
        queueScore[to] = queueScore[from];
        queueCost[to] = queueCost[from];
        queueState[to] = queueState[from];
    }

    private void place(int index, double score, double cost, int state) {
        queueScore[index] = score;
        queueCost[index] = cost;
        queueState[index] = state;
    }

    private void grow() {
        int size = queueScore.length * 2;
        queueScore = Arrays.copyOf(queueScore, size);
        queueCost = Arrays.copyOf(queueCost, size);
        queueState = Arrays.copyOf(queueState, size);
    }
}
