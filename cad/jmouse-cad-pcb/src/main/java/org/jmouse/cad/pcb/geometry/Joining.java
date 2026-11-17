package org.jmouse.cad.pcb.geometry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * Which pieces of copper are one, and through which touches: the pieces joined in groups, each group named by its first piece —
 * pads come first —, so a group is named the same however the joins came; and every touch kept, so where a group joins two
 * nets can be told — not only that it does.
 */
final class Joining {

    private final int[]       parents;
    /** Every touch, as the two pieces that touch: what a short is traced through. */
    private final List<int[]> touches = new ArrayList<>();

    Joining(int pieces) {
        this.parents = IntStream.range(0, pieces).toArray();
    }

    /** The group a piece is in: its first piece. */
    int groupOf(int piece) {
        int root = piece;
        while (parents[root] != root) {
            parents[root] = parents[parents[root]];
            root = parents[root];
        }
        return root;
    }

    /** Two pieces that touch: one group from now on, its first piece naming it. */
    void join(int one, int other) {
        touches.add(new int[] {one, other});
        int first = groupOf(one);
        int second = groupOf(other);
        parents[Math.max(first, second)] = Math.min(first, second);
    }

    /** Every piece's group, worked out once. */
    int[] groups() {
        int[] groups = new int[parents.length];
        for (int piece = 0; piece < groups.length; piece++) {
            groups[piece] = groupOf(piece);
        }
        return groups;
    }

    /**
     * Copper that joins two nets: the pieces from one that names the first net — a pad of the circuit, a zone, a piece drawn with
     * its net — through what touches, to one that names the second, each touching the next.
     */
    record Chain(List<Integer> pieces, String firstNet, String secondNet) {
    }

    /**
     * Where a group joins nets: for each two of its nets, the chain of fewest touches between a piece naming one and a piece
     * naming the other — copper floods out from every piece that names a net, each piece reached through the fewest touches;
     * where two floods of different nets touch, the way back along each is the chain.
     *
     * @param group the group, by its first piece
     * @param netOf the net a piece names itself, empty for one that names none
     */
    List<Chain> chainsIn(int group, int[] groups, IntFunction<Optional<String>> netOf) {
        Map<Integer, List<Integer>> neighbours = new HashMap<>();
        for (int[] touch : touches) {
            if (groups[touch[0]] == group) {
                neighbours.computeIfAbsent(touch[0], ignored -> new ArrayList<>()).add(touch[1]);
                neighbours.computeIfAbsent(touch[1], ignored -> new ArrayList<>()).add(touch[0]);
            }
        }
        Map<Integer, String> reachedFrom = new HashMap<>();
        Map<Integer, Integer> cameFrom = new HashMap<>();
        Deque<Integer> flooding = new ArrayDeque<>();
        for (int piece = 0; piece < groups.length; piece++) {
            int named = piece;
            if (groups[piece] == group) {
                netOf.apply(piece).ifPresent(net -> {
                    reachedFrom.put(named, net);
                    flooding.add(named);
                });
            }
        }
        Map<Set<String>, Chain> chains = new LinkedHashMap<>();
        while (!flooding.isEmpty()) {
            int piece = flooding.poll();
            String net = reachedFrom.get(piece);
            for (int next : neighbours.getOrDefault(piece, List.of())) {
                String other = reachedFrom.get(next);
                if (other == null) {
                    reachedFrom.put(next, net);
                    cameFrom.put(next, piece);
                    flooding.add(next);
                } else if (!other.equals(net)) {
                    chains.putIfAbsent(Set.of(net, other), new Chain(chainThrough(piece, next, cameFrom), net, other));
                }
            }
        }
        return List.copyOf(chains.values());
    }

    /** The way from the piece one flood began at to a piece of it, on through a touching piece back to where the other began. */
    private static List<Integer> chainThrough(int mine, int theirs, Map<Integer, Integer> cameFrom) {
        List<Integer> chain = new ArrayList<>();
        for (Integer at = mine; at != null; at = cameFrom.get(at)) {
            chain.addFirst(at);
        }
        for (Integer at = theirs; at != null; at = cameFrom.get(at)) {
            chain.add(at);
        }
        return chain;
    }
}
