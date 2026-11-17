package org.jmouse.cad.circuit;

import org.jspecify.annotations.Nullable;
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
     * @param reference   the part it places, as the circuit names it
     * @param unit        the block placed on its own, {@code B}; null for the part as a whole
     * @param at          where its centre stands
     * @param rotation    how far it is turned, degrees
     * @param mirrored    whether it is drawn mirrored
     * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
     * @param name     where its reference is written, from its centre, or null where the sheet puts it
     * @param value    where its value is written, from its centre, or null
     * @param readings where an instrument's readings are written, from its centre, or null
     * @param size     a placed sheet's box, as KiCad sizes a hierarchical sheet, or null for a part drawn by its symbol
     * @param shows    what an instrument's display shows — {@code rms}, {@code graph} —, as written
     * @param ports    where a placed sheet's ports stand on its frame, as written; a port it does not place stands where
     *                 the sheet puts it
     */
    record PlacedPart(String reference, @Nullable String unit, Point at, double rotation, boolean mirrored, @Nullable Point name, @Nullable Point value,
                      @Nullable Point readings, @Nullable Point size, List<String> shows, List<PortPlace> ports, Arrangement arrangement) implements Placement {

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
     * @param reference   the name its line is known by — {@code R1}, {@code wire-3} —, unique on the sheet
     * @param symbol      what it is — {@code junction}, {@code ground}, {@code label}… —, as written
     * @param net         the net it names — {@code label vp}, {@code ground agnd} —, or null
     * @param at          where it stands
     * @param rotation    how far it is turned, degrees
     * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
     */
    record Mark(String reference, String symbol, @Nullable String net, Point at, double rotation, Arrangement arrangement)
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
     * @param reference   the name its line is known by — {@code R1}, {@code wire-3} —, unique on the sheet
     * @param words       what it says
     * @param at          where it stands
     * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
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
     * @param reference   the name its line is known by — {@code R1}, {@code wire-3} —, unique on the sheet
     * @param shape       what it is
     * @param via         its points: a line's corners, a rectangle's two opposite corners, a circle's centre and a point on it, an
     *                    arc's two ends and a point it goes through
     * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
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
     * A line of a bus: the thick line it is drawn as on the sheet. It names one of the circuit's {@link Bus buses}, which says what
     * it carries; a sheet may draw one bus as several lines. It joins nothing by itself — an entry on it is a mark of a member's net.
     *
     * @param reference   the name the line is known by — {@code B1} —, unique on the sheet
     * @param bus         the bus it draws, by name
     * @param via         its corners, in order
     * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
     */
    record BusLine(String reference, String bus, List<Point> via, Arrangement arrangement) implements Placement {

        public BusLine {
            via = List.copyOf(via);
            arrangement = arrangement == null ? Arrangement.FREE : arrangement;
        }

        @Override
        public BusLine arranged(Arrangement other) {
            return new BusLine(reference, bus, via, other);
        }
    }

    /**
     * A wire from one pin to another — {@code R1.negative} —, through its corners.
     *
     * @param reference   the name its line is known by — {@code R1}, {@code wire-3} —, unique on the sheet
     * @param from        where it starts, as its line writes it: a part's pin — {@code R1.negative} — or a mark, by its reference
     * @param to          where it ends, written as {@code from} is
     * @param via         its corners between its ends, in order
     * @param arrangement the group it is chosen and moved with, and whether it is locked where it stands
     */
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
