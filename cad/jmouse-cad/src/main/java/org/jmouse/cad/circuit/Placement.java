package org.jmouse.cad.circuit;

import org.jmouse.cad.Point;
import org.jmouse.cad.drawing.Arrangement;

import java.util.List;

/** Where something of a circuit is drawn on its sheet: the {@code layout} block's lines. */
public sealed interface Placement {

    String reference();

    /** The group it is chosen and moved with, and whether it is locked where it stands. */
    Arrangement arrangement();

    /** The same, standing otherwise among the others. */
    Placement arranged(Arrangement other);

    /**
     * A part where it stands — or one block of it, placed on its own: {@code U4.B at …}.
     *
     * @param unit     the block placed on its own, {@code B}; null for the part as a whole
     * @param name     where its reference is written, from its centre, or null where the sheet puts it
     * @param value    where its value is written, from its centre, or null
     * @param readings where an instrument's readings are written, from its centre, or null
     * @param size     a placed sheet's box, as KiCad sizes a hierarchical sheet, or null for a part drawn by its symbol
     * @param shows    what an instrument's display shows — {@code rms}, {@code graph} —, as written
     * @param ports    where a placed sheet's ports stand on its frame, as written; a port it does not place stands where
     *                 the sheet puts it
     */
    record PlacedPart(String reference, String unit, Point at, double rotation, boolean mirrored, Point name, Point value,
                      Point readings, Point size, List<String> shows, List<PortPlace> ports, Arrangement arrangement) implements Placement {

        public PlacedPart {
            shows = List.copyOf(shows);
            ports = ports == null ? List.of() : List.copyOf(ports);
            arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        }

        public PlacedPart(String reference, String unit, Point at, double rotation, boolean mirrored, Point name, Point value,
                          Point readings, Point size, List<String> shows) {
            this(reference, unit, at, rotation, mirrored, name, value, readings, size, shows, List.of(), Arrangement.FREE);
        }

        @Override
        public PlacedPart arranged(Arrangement other) {
            return new PlacedPart(reference, unit, at, rotation, mirrored, name, value, readings, size, shows, ports, other);
        }

        /** How a refusal names it: the part, or one block of it. */
        public String placedName() {
            return unit == null ? reference : reference + "." + unit;
        }
    }

    /**
     * Where a port of a placed sheet stands on the sheet's frame — {@code in left 60} —: the edge, and how far along it
     * from its start, the top of a side edge and the left of the top and bottom ones. KiCad draws a hierarchical sheet's
     * pins wherever its author put them, and a wire drawn to one comes back to the same point only if the port does.
     *
     * @param port  the port, by its net on the sheet placed
     * @param edge  the frame's edge it stands on
     * @param along how far from the edge's start, in the sheet's units
     */
    record PortPlace(String port, Edge edge, double along) {

        /** A frame's edge a port stands on. */
        public enum Edge {
            LEFT, RIGHT, TOP, BOTTOM;

            /** The word the language writes it as. */
            public String word() {
                return name().toLowerCase(java.util.Locale.ROOT);
            }

            /** The edge a word names; a word that names none is refused, saying what may stand there. */
            public static Edge named(String word) {
                for (Edge edge : values()) {
                    if (edge.word().equals(word)) {
                        return edge;
                    }
                }
                throw new IllegalArgumentException("A port stands on the left, right, top or bottom edge, not '" + word + "'");
            }
        }
    }

    /**
     * A mark that is no part: a junction, a ground, a common, a label, a power flag.
     *
     * @param symbol what it is — {@code junction}, {@code ground}, {@code label}… —, as written
     * @param net    the net it names — {@code label vp}, {@code ground agnd} —, or null
     */
    record Mark(String reference, String symbol, String net, Point at, double rotation, Arrangement arrangement)
            implements Placement {

        public Mark {
            arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        }

        @Override
        public Mark arranged(Arrangement other) {
            return new Mark(reference, symbol, net, at, rotation, other);
        }
    }

    /**
     * Words written on a sheet for whoever reads it: they join nothing.
     *
     * @param words what it says
     */
    record Note(String reference, String words, Point at, Arrangement arrangement) implements Placement {

        public Note {
            arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        }

        @Override
        public Note arranged(Arrangement other) {
            return new Note(reference, words, at, other);
        }
    }

    /**
     * A line or a shape drawn on a sheet for whoever reads it: it joins nothing.
     *
     * @param shape what it is
     * @param via   its points: a line's corners, a rectangle's two opposite corners, a circle's centre and a point on it, an
     *              arc's two ends and a point it goes through
     */
    record Drawn(String reference, Shape shape, List<Point> via, Arrangement arrangement) implements Placement {

        public Drawn {
            via = List.copyOf(via);
            arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        }

        @Override
        public Drawn arranged(Arrangement other) {
            return new Drawn(reference, shape, via, other);
        }

        /** What can be drawn on a sheet. */
        public enum Shape {
            LINE, RECTANGLE, CIRCLE, ARC;

            /** The word the language writes it as. */
            public String word() {
                return name().toLowerCase(java.util.Locale.ROOT);
            }

            /** The shape a word names; a word that names none is refused, saying what may be drawn. */
            public static Shape named(String word) {
                for (Shape shape : values()) {
                    if (shape.word().equals(word)) {
                        return shape;
                    }
                }
                throw new IllegalArgumentException("A sheet draws a line, a rectangle, a circle or an arc, not '" + word + "'");
            }
        }
    }

    /**
     * A bus: a thick line carrying a named set of nets. It joins nothing by itself; the net labels on it do.
     *
     * @param members the nets it carries; empty while it has only a name
     * @param via     its corners, in order
     */
    record Bus(String reference, String name, List<String> members, List<Point> via, Arrangement arrangement) implements Placement {

        public Bus {
            members = List.copyOf(members);
            via = List.copyOf(via);
            arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        }

        @Override
        public Bus arranged(Arrangement other) {
            return new Bus(reference, name, members, via, other);
        }
    }

    /** A wire from one pin to another — {@code R1.negative} —, through its corners. */
    record Wire(String reference, String from, String to, List<Point> via, Arrangement arrangement) implements Placement {

        public Wire {
            via = List.copyOf(via);
            arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        }

        @Override
        public Wire arranged(Arrangement other) {
            return new Wire(reference, from, to, via, other);
        }
    }
}
