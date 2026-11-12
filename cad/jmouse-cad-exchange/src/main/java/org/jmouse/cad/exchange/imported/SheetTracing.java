package org.jmouse.cad.exchange.imported;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Another product's wires as jMX's: its segments cut wherever a connection point lies on them, and every chain of
 * segments between two terminals one wire whose corners are its bends. A point where three segments meet with no terminal
 * on it stands for a junction of its own; so does the loose end of a wire. Points are the product's own, in millimetres —
 * what they are drawn as is the import's.
 */
public final class SheetTracing {

    /** A point of the product's sheet, millimetres: two closer than a nanometre are one. */
    public record Point(double x, double y) {

        private static final double SAME = 1e-6;

        public boolean sameAs(Point other) {
            return Math.abs(x - other.x) < SAME && Math.abs(y - other.y) < SAME;
        }

        /** Whether it lies on the segment between two points — on one of its ends or anywhere between. */
        public boolean liesOn(Point start, Point end) {
            double across = (end.x - start.x) * (y - start.y) - (end.y - start.y) * (x - start.x);
            double length = Math.hypot(end.x - start.x, end.y - start.y);
            if (length < SAME) {
                return sameAs(start);
            }
            if (Math.abs(across) / length > SAME) {
                return false;
            }
            double along = (x - start.x) * (end.x - start.x) + (y - start.y) * (end.y - start.y);
            return along >= -SAME * length && along <= length * length + SAME * length;
        }

        /** What tells it apart from any other point, to the nanometre. */
        public String key() {
            return Math.round(x / SAME) + "," + Math.round(y / SAME);
        }

        private double distanceTo(Point other) {
            return Math.hypot(x - other.x, y - other.y);
        }
    }

    /** A wire from the terminal on one point to the terminal on another, through its corners. */
    public record Chain(Point from, List<Point> bends, Point to) {

        public Chain {
            bends = List.copyOf(bends);
        }
    }

    private SheetTracing() {
    }

    /**
     * The chains between terminals, from each terminal in turn.
     *
     * @param terminals the points terminals stand on, in the order their chains are walked from
     * @param wires     the product's wires, each through its corners
     * @param junction  called for each point that stands for a junction — a meeting of three or more, a loose end —; its
     *                  terminal is walked from after the others
     */
    public static List<Chain> trace(List<Point> terminals, List<List<Point>> wires, Consumer<Point> junction) {
        Map<String, Point> places = new LinkedHashMap<>();
        Map<String, Set<String>> neighbours = new LinkedHashMap<>();
        List<Point> stops = new ArrayList<>(terminals);
        wires.forEach(stops::addAll);
        for (List<Point> wire : wires) {
            for (int index = 1; index < wire.size(); index++) {
                Point start = wire.get(index - 1);
                Point end = wire.get(index);
                List<Point> along = new ArrayList<>();
                for (Point stop : stops) {
                    if (stop.liesOn(start, end) && !stop.sameAs(start) && !stop.sameAs(end)) {
                        along.add(stop);
                    }
                }
                along.sort((one, other) -> Double.compare(start.distanceTo(one), start.distanceTo(other)));
                Point previous = start;
                along.add(end);
                for (Point next : along) {
                    if (!next.sameAs(previous)) {
                        places.put(previous.key(), previous);
                        places.put(next.key(), next);
                        neighbours.computeIfAbsent(previous.key(), ignored -> new LinkedHashSet<>()).add(next.key());
                        neighbours.computeIfAbsent(next.key(), ignored -> new LinkedHashSet<>()).add(previous.key());
                    }
                    previous = next;
                }
            }
        }
        Set<String> ends = new LinkedHashSet<>();
        terminals.forEach(terminal -> ends.add(terminal.key()));
        neighbours.forEach((key, around) -> {
            if (!ends.contains(key) && around.size() != 2) {
                ends.add(key);
                junction.accept(places.get(key));
            }
        });
        List<Chain> chains = new ArrayList<>();
        Set<String> walked = new HashSet<>();
        for (String start : ends) {
            for (String first : neighbours.getOrDefault(start, Set.of())) {
                if (walked.contains(edge(start, first))) {
                    continue;
                }
                List<Point> bends = new ArrayList<>();
                String previous = start;
                String current = first;
                walked.add(edge(previous, current));
                while (!ends.contains(current)) {
                    bends.add(places.get(current));
                    String came = previous;
                    Optional<String> next = neighbours.get(current).stream().filter(key -> !key.equals(came)).findFirst();
                    if (next.isEmpty()) {
                        break;
                    }
                    previous = current;
                    current = next.get();
                    walked.add(edge(previous, current));
                }
                if (ends.contains(current) && !current.equals(start)) {
                    chains.add(new Chain(places.get(start), bends, places.get(current)));
                }
            }
        }
        return chains;
    }

    private static String edge(String one, String other) {
        return one.compareTo(other) < 0 ? one + "|" + other : other + "|" + one;
    }
}
