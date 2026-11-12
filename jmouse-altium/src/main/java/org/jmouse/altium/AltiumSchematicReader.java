package org.jmouse.altium;

import org.jmouse.altium.AltiumSchematic.Component;
import org.jmouse.altium.AltiumSchematic.Graphic;
import org.jmouse.altium.AltiumSchematic.Orientation;
import org.jmouse.altium.AltiumSchematic.Pin;
import org.jmouse.altium.AltiumSchematic.Point;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * An Altium schematic read into its model: each record by its kind, what a component owns — its pins, its drawing, its
 * designator, its parameters, its footprint — gathered under it by {@code OWNERINDEX}, only of the part it places and
 * the drawing it shows. What the model does not hold — sheet symbols, buses, images — is counted in {@code leftOut}.
 */
public final class AltiumSchematicReader {

    /** {@code PINCONGLOMERATE}: its turn in the low two bits, then whether it is hidden, its name shown, its designator. */
    private static final int ORIENTATION_BITS = 0x03;
    private static final int HIDDEN = 0x04;
    private static final int NAME_SHOWN = 0x08;
    private static final int DESIGNATOR_SHOWN = 0x10;

    /** An outer edge's inverting dot, an inner edge's clock wedge. */
    private static final int OUTER_EDGE_DOT = 1;
    private static final int INNER_EDGE_CLOCK = 3;

    /** The parameter a component says what it is by. */
    private static final String COMMENT = "Comment";

    /** The kinds of record this reading takes, by Altium's number. */
    private enum Kind {
        COMPONENT(1), PIN(2), BEZIER(5), POLYLINE(6), POLYGON(7), ELLIPSE(8), ROUND_RECTANGLE(10), ELLIPTICAL_ARC(11), ARC(12),
        LINE(13), RECTANGLE(14), SHEET_SYMBOL(15), POWER_PORT(17), PORT(18), NET_LABEL(25), BUS(26), WIRE(27), JUNCTION(29),
        IMAGE(30), DESIGNATOR(34), PARAMETER(41), IMPLEMENTATION_LIST(44), IMPLEMENTATION(45), OTHER(-1);

        private final int number;

        Kind(int number) {
            this.number = number;
        }

        static Kind of(int number) {
            for (Kind kind : values()) {
                if (kind.number == number) {
                    return kind;
                }
            }
            return OTHER;
        }
    }

    private AltiumSchematicReader() {
    }

    /** @throws AltiumReadException when the bytes are no Altium schematic, or one broken past reading */
    public static AltiumSchematic read(String name, byte[] bytes) {
        List<Map<String, String>> written = AltiumRecords.read(name, bytes);
        List<AltiumRecord> records = new ArrayList<>();
        for (int index = 1; index < written.size(); index++) {
            records.add(new AltiumRecord(index - 1, written.get(index)));
        }
        return new Reading(records).schematic();
    }

    private static final class Reading {

        private final List<AltiumRecord> records;
        private final Map<Integer, List<AltiumRecord>> owned = new LinkedHashMap<>();
        private final Map<String, Integer> leftOut = new TreeMap<>();

        Reading(List<AltiumRecord> records) {
            this.records = records;
            for (AltiumRecord record : records) {
                int owner = record.integer("OWNERINDEX", -1);
                if (owner >= 0) {
                    owned.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(record);
                }
            }
        }

        AltiumSchematic schematic() {
            List<Component> components = new ArrayList<>();
            List<AltiumSchematic.Wire> wires = new ArrayList<>();
            List<AltiumSchematic.Bus> buses = new ArrayList<>();
            List<AltiumSchematic.NetLabel> labels = new ArrayList<>();
            List<AltiumSchematic.PowerPort> powerPorts = new ArrayList<>();
            List<AltiumSchematic.Port> ports = new ArrayList<>();
            List<Point> junctions = new ArrayList<>();
            for (AltiumRecord record : records) {
                if (record.integer("OWNERINDEX", -1) >= 0) {
                    continue;
                }
                switch (Kind.of(record.kind())) {
                    case COMPONENT -> components.add(component(record));
                    case WIRE -> wires.add(new AltiumSchematic.Wire(points(record)));
                    case NET_LABEL -> labels.add(new AltiumSchematic.NetLabel(record.text("TEXT", ""), location(record)));
                    case POWER_PORT -> powerPorts.add(new AltiumSchematic.PowerPort(record.text("TEXT", ""), location(record),
                            Orientation.of(record.integer("ORIENTATION", 0)),
                            AltiumSchematic.PowerStyle.of(record.integer("STYLE", 0))));
                    case PORT -> ports.add(new AltiumSchematic.Port(record.text("NAME", ""), location(record)));
                    case JUNCTION -> junctions.add(location(record));
                    case SHEET_SYMBOL -> leave("sheet symbol");
                    case BUS -> buses.add(new AltiumSchematic.Bus(points(record)));
                    case IMAGE -> leave("image");
                    default -> {
                    }
                }
            }
            List<String> notes = new ArrayList<>();
            leftOut.forEach((what, count) -> notes.add(count + " × " + what));
            return new AltiumSchematic(components, wires, buses, labels, powerPorts, ports, junctions, notes);
        }

        private void leave(String what) {
            leftOut.merge(what, 1, Integer::sum);
        }

        /** A component and what it owns of the part it places and the drawing it shows. */
        private Component component(AltiumRecord record) {
            int part = record.integer("CURRENTPARTID", 1);
            int mode = record.integer("DISPLAYMODE", 0);
            List<Pin> pins = new ArrayList<>();
            List<Graphic> graphics = new ArrayList<>();
            String designator = "";
            Optional<Point> designatorAt = Optional.empty();
            Optional<Point> commentAt = Optional.empty();
            Map<String, String> parameters = new LinkedHashMap<>();
            Optional<String> footprint = Optional.empty();
            for (AltiumRecord child : owned.getOrDefault(record.index(), List.of())) {
                Kind kind = Kind.of(child.kind());
                boolean shown = shownIn(child, part, mode);
                switch (kind) {
                    case PIN -> {
                        if (shown) {
                            pins.add(pin(child));
                        }
                    }
                    case DESIGNATOR -> {
                        designator = child.text("TEXT", "");
                        designatorAt = writtenAt(child);
                    }
                    case PARAMETER -> {
                        parameters.put(child.text("NAME", ""), child.text("TEXT", ""));
                        if (child.text("NAME", "").equals(COMMENT)) {
                            commentAt = writtenAt(child);
                        }
                    }
                    case IMPLEMENTATION_LIST -> footprint = footprint.or(() -> footprintOf(child));
                    default -> {
                        if (shown) {
                            graphic(kind, child).ifPresent(graphics::add);
                        }
                    }
                }
            }
            return new Component(record.index(), record.text("LIBREFERENCE", ""),
                    Optional.of(record.text("SOURCELIBRARYNAME", "")).filter(library -> !library.isBlank()), record.text("COMPONENTDESCRIPTION", ""),
                    designator, comment(record, parameters), location(record), Orientation.of(record.integer("ORIENTATION", 0)),
                    record.flag("ISMIRRORED"), part, record.integer("PARTCOUNT", 2), mode, footprint, designatorAt, commentAt, pins,
                    graphics);
        }

        /** Whether a record of a component is of the part placed — a part of -1 or 0 is every part's — and the drawing shown. */
        private static boolean shownIn(AltiumRecord child, int part, int mode) {
            int ownerPart = child.integer("OWNERPARTID", -1);
            return (ownerPart <= 0 || ownerPart == part) && child.integer("OWNERPARTDISPLAYMODE", 0) == mode;
        }

        /** What the component says it is: its Comment, which may name another parameter of it — {@code =Value}. */
        private static String comment(AltiumRecord record, Map<String, String> parameters) {
            String comment = parameters.getOrDefault(COMMENT, record.text("COMMENT", ""));
            if (comment.startsWith("=")) {
                return parameters.getOrDefault(comment.substring(1), comment);
            }
            return comment;
        }

        /** Where a designator or a parameter is written; nowhere when it is hidden. */
        private static Optional<Point> writtenAt(AltiumRecord record) {
            return record.flag("ISHIDDEN") ? Optional.empty() : Optional.of(location(record));
        }

        /** The PCB model a component's implementation list holds current. */
        private Optional<String> footprintOf(AltiumRecord list) {
            return owned.getOrDefault(list.index(), List.of()).stream()
                    .filter(model -> Kind.of(model.kind()) == Kind.IMPLEMENTATION)
                    .filter(model -> model.text("MODELTYPE", "").equalsIgnoreCase("PCBLIB") && model.flag("ISCURRENT"))
                    .map(model -> model.text("MODELNAME", "")).filter(modelName -> !modelName.isBlank()).findFirst();
        }

        private static Pin pin(AltiumRecord record) {
            int conglomerate = record.integer("PINCONGLOMERATE", 0);
            return new Pin(record.text("NAME", ""), record.text("DESIGNATOR", ""),
                    AltiumSchematic.Electrical.of(record.integer("ELECTRICAL", 4)), location(record),
                    Orientation.of(conglomerate & ORIENTATION_BITS), record.coordinate("PINLENGTH"),
                    (conglomerate & HIDDEN) != 0, (conglomerate & NAME_SHOWN) != 0, (conglomerate & DESIGNATOR_SHOWN) != 0,
                    record.integer("SYMBOL_OUTEREDGE", 0) == OUTER_EDGE_DOT,
                    record.integer("SYMBOL_INNEREDGE", 0) == INNER_EDGE_CLOCK);
        }

        private Optional<Graphic> graphic(Kind kind, AltiumRecord record) {
            double width = record.integer("LINEWIDTH", 1);
            return Optional.ofNullable(switch (kind) {
                case LINE -> new Graphic.Polyline(List.of(location(record), corner(record)), width);
                case POLYLINE -> new Graphic.Polyline(points(record), width);
                case POLYGON -> new Graphic.Polygon(points(record), record.flag("ISSOLID"));
                case RECTANGLE, ROUND_RECTANGLE -> new Graphic.Rectangle(location(record), corner(record), record.flag("ISSOLID"));
                case ELLIPSE -> new Graphic.Ellipse(location(record), record.coordinate("RADIUS"),
                        record.text("SECONDARYRADIUS").isPresent() ? record.coordinate("SECONDARYRADIUS") : record.coordinate("RADIUS"),
                        record.flag("ISSOLID"));
                case ARC, ELLIPTICAL_ARC -> new Graphic.Arc(location(record), record.coordinate("RADIUS"),
                        real(record, "STARTANGLE"), real(record, "ENDANGLE"));
                case BEZIER -> new Graphic.Bezier(points(record));
                default -> null;
            });
        }

        private static double real(AltiumRecord record, String key) {
            return record.text(key).map(String::strip).map(Double::parseDouble).orElse(0.0);
        }

        private static Point location(AltiumRecord record) {
            return new Point(record.coordinate("LOCATION.X"), record.coordinate("LOCATION.Y"));
        }

        private static Point corner(AltiumRecord record) {
            return new Point(record.coordinate("CORNER.X"), record.coordinate("CORNER.Y"));
        }

        /** {@code LOCATIONCOUNT} points, {@code X1}/{@code Y1} on. */
        private static List<Point> points(AltiumRecord record) {
            List<Point> points = new ArrayList<>();
            int count = record.integer("LOCATIONCOUNT", 0);
            for (int index = 1; index <= count; index++) {
                points.add(new Point(record.coordinate("X" + index), record.coordinate("Y" + index)));
            }
            return points;
        }
    }
}
