package org.jmouse.altium;

import java.util.List;
import java.util.Optional;

/**
 * An Altium schematic sheet as its records say it, in Altium's units — ten mils — with Y upwards: its components, each
 * with its pins and its drawing as it stands on the sheet, its wires and bus lines, net labels, power ports, ports and junctions.
 */
public record AltiumSchematic(List<Component> components, List<Wire> wires, List<Bus> buses, List<NetLabel> netLabels,
                              List<PowerPort> powerPorts, List<Port> ports, List<Point> junctions, List<String> leftOut) {

    public AltiumSchematic {
        components = List.copyOf(components);
        wires = List.copyOf(wires);
        buses = List.copyOf(buses);
        netLabels = List.copyOf(netLabels);
        powerPorts = List.copyOf(powerPorts);
        ports = List.copyOf(ports);
        junctions = List.copyOf(junctions);
        leftOut = List.copyOf(leftOut);
    }

    /** A point of the sheet, in units of ten mils. */
    public record Point(double x, double y) {
    }

    /** Which way a thing points: a pin from its body, a port, a part's turn — a quarter turn counter-clockwise a step. */
    public enum Orientation {
        RIGHT, UP, LEFT, DOWN;

        public static Orientation of(int written) {
            return values()[Math.floorMod(written, 4)];
        }

        /** Its angle, degrees counter-clockwise. */
        public int degrees() {
            return ordinal() * 90;
        }
    }

    /**
     * A placed component: what library part it is, its designator and the part of it placed, its turn, and the footprint
     * its current model names.
     *
     * @param sourceLibrary the library it was placed from, as the sheet names it ({@code Miscellaneous Devices.IntLib}), or empty
     * @param currentPart the part of a multi-part component this record places, from 1
     * @param partCount   how many parts it has: Altium writes one more than there are
     * @param displayMode which of its drawings is shown: 0 the normal one
     * @param footprint   the PCB model it is current with, or empty
     * @param designatorAt where its designator is written, when it is
     * @param commentAt    where its comment is written, when it is
     * @param pins        its pins of the part placed and the drawing shown, in the file's order
     * @param graphics    its drawing of the part placed and the drawing shown, already where it stands on the sheet
     */
    public record Component(int index, String libraryReference, Optional<String> sourceLibrary, String description, String designator, String comment,
                            Point location, Orientation orientation, boolean mirrored, int currentPart, int partCount,
                            int displayMode, Optional<String> footprint, Optional<Point> designatorAt,
                            Optional<Point> commentAt, List<Pin> pins, List<Graphic> graphics) {

        public Component {
            pins = List.copyOf(pins);
            graphics = List.copyOf(graphics);
        }

        /** Whether it has more than one part: Altium's part count says one more than there are. */
        public boolean multiPart() {
            return partCount > 2;
        }
    }

    /** What a pin carries, as Altium's electrical type says it. */
    public enum Electrical {
        INPUT, BIDIRECTIONAL, OUTPUT, OPEN_COLLECTOR, PASSIVE, HIGH_IMPEDANCE, OPEN_EMITTER, POWER;

        public static Electrical of(int written) {
            return written >= 0 && written < values().length ? values()[written] : PASSIVE;
        }
    }

    /**
     * A pin: where it meets the body, which way it points from there and how long it is — its hot end, where a wire
     * connects, is that far that way —, what it is called and its package designator.
     *
     * @param inverted whether its outer edge carries an inverting dot
     * @param clocked  whether its inner edge carries a clock's wedge
     */
    public record Pin(String name, String designator, Electrical electrical, Point location, Orientation orientation,
                      double length, boolean hidden, boolean nameShown, boolean designatorShown, boolean inverted,
                      boolean clocked) {

        /** Where a wire connects: its end away from the body. */
        public Point end() {
            return switch (orientation) {
                case RIGHT -> new Point(location.x() + length, location.y());
                case UP -> new Point(location.x(), location.y() + length);
                case LEFT -> new Point(location.x() - length, location.y());
                case DOWN -> new Point(location.x(), location.y() - length);
            };
        }
    }

    /** What a component draws, already where it stands on the sheet. */
    public sealed interface Graphic {

        /** A straight line, or a run of them. */
        record Polyline(List<Point> points, double width) implements Graphic {
            public Polyline {
                points = List.copyOf(points);
            }
        }

        /** A closed area through its points, filled where {@code solid}. */
        record Polygon(List<Point> points, boolean solid) implements Graphic {
            public Polygon {
                points = List.copyOf(points);
            }
        }

        /** A rectangle between two corners — a round-cornered one too —, filled where {@code solid}. */
        record Rectangle(Point corner, Point opposite, boolean solid) implements Graphic {
        }

        /** An ellipse round its centre — a circle where both radii agree —, filled where {@code solid}. */
        record Ellipse(Point centre, double radius, double secondaryRadius, boolean solid) implements Graphic {
        }

        /** An arc of a circle round its centre, from its start angle counter-clockwise to its end, degrees. */
        record Arc(Point centre, double radius, double startAngle, double endAngle) implements Graphic {
        }

        /** A cubic Bézier through its four points, a run of them sharing ends. */
        record Bezier(List<Point> points) implements Graphic {
            public Bezier {
                points = List.copyOf(points);
            }
        }
    }

    /** A wire: a run of straight stretches through its points. */
    public record Wire(List<Point> points) {
        public Wire {
            points = List.copyOf(points);
        }
    }

    /** A bus line: a run of straight stretches through its points, named by a net label standing on it ({@code D[0..7]}). */
    public record Bus(List<Point> points) {
        public Bus {
            points = List.copyOf(points);
        }
    }

    /** A net's name written where a wire takes it — or, on a bus line, the bus's name and the nets it carries. */
    public record NetLabel(String text, Point location) {
    }

    /** How a power port is drawn: an arrow, a bar or a wave for a supply, the grounds for one. */
    public enum PowerStyle {
        CIRCLE, ARROW, BAR, WAVE, POWER_GROUND, SIGNAL_GROUND, EARTH, GOST_ARROW, GOST_POWER_GROUND, GOST_EARTH, GOST_BAR;

        public static PowerStyle of(int written) {
            return written >= 0 && written < values().length ? values()[written] : BAR;
        }

        /** Whether it is drawn as a ground. */
        public boolean ground() {
            return this == POWER_GROUND || this == SIGNAL_GROUND || this == EARTH || this == GOST_POWER_GROUND || this == GOST_EARTH;
        }
    }

    /** A power port: the net it joins every port of its name to, where it connects, which way it is drawn. */
    public record PowerPort(String net, Point location, Orientation orientation, PowerStyle style) {
    }

    /** A sheet port: the net it hands across the sheet's edge, where it stands. */
    public record Port(String name, Point location) {
    }
}
