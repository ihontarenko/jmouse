package org.jmouse.kicad.connectivity;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.KiCadProject;
import org.jmouse.kicad.KiCadProject.SheetInstance;
import org.jmouse.kicad.schematic.KiCadLabel;
import org.jmouse.kicad.schematic.KiCadSchematic;
import org.jmouse.kicad.schematic.KiCadSheet;
import org.jmouse.kicad.schematic.KiCadSymbolInstance;
import org.jmouse.kicad.schematic.KiCadWire;
import org.jmouse.kicad.symbol.KiCadPin;
import org.jmouse.kicad.symbol.KiCadSymbolDefinition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The nets of a design, as KiCad computes them.
 *
 * <ul>
 *     <li>On a sheet, everything whose connection point is the same point is joined — a pin, a wire's end, a label, a
 *     junction, a sheet box's pin —, and so is everything whose point lies on a wire, at its end or in its middle: a
 *     wire ending on another makes a T. Two wires crossing in their middles do not join unless a junction stands there.</li>
 *     <li>Across sheets: a global label with every one of its name; a power symbol with every one of its value; a
 *     hidden power input with the net of its name; a hierarchical label with the pin of its name on the box that
 *     places its sheet; a local label with those of its name on the same sheet instance.</li>
 *     <li>A net takes a power symbol's or a global label's name, else a label's under its sheet's path, else
 *     {@code Net-(R5-Pad1)}.</li>
 * </ul>
 *
 * <p>Buses and bus entries are not read: a design that joins by them reads as if they were not drawn.</p>
 */
public final class KiCadConnectivity {

    private final List<KiCadNet> nets;
    private final List<KiCadConnection> unconnected;
    /** The net every connection point stands on, by its key: a label, a junction, a wire's corner. */
    private final Map<String, String> pointNets;

    private KiCadConnectivity(List<KiCadNet> nets, List<KiCadConnection> unconnected, Map<String, String> pointNets) {
        this.pointNets = Map.copyOf(pointNets);
        this.nets = List.copyOf(nets);
        this.unconnected = List.copyOf(unconnected);
    }

    /** The nets of a whole design. */
    public static KiCadConnectivity of(KiCadProject project) {
        Joining joining = new Joining();
        for (SheetInstance sheet : project.sheets()) {
            joining.sheet(sheet);
        }
        return joining.result();
    }

    /** Every net that joins at least one pin, ordered by name. */
    public List<KiCadNet> nets() {
        return nets;
    }

    /**
     * The net a connection point of a sheet instance stands on — a label's, a junction's, a sheet box pin's, a wire's
     * corner —, by the name {@link #nets} gives it; empty for a point nothing connects, or a group no pin is on and no
     * label names.
     *
     * @param sheetPath the instance's path, {@code /root-uuid/sheet-uuid}
     */
    public Optional<String> netAt(String sheetPath, KiCadPoint at) {
        return Optional.ofNullable(pointNets.get(Joining.point(sheetPath, at)));
    }

    /** The pins nothing joins and no no-connect mark excuses: what KiCad's rules check reports as unconnected. */
    public List<KiCadConnection> unconnected() {
        return unconnected;
    }

    /** One pass over a design: a union of keys, and what each key stands for. */
    private static final class Joining {

        private final Map<String, String> parents = new HashMap<>();
        /** The names a group of keys may take, by key, the strongest first: power and global, then labels. */
        private final Map<String, List<Naming>> names = new HashMap<>();
        private final Map<String, KiCadConnection> pins = new LinkedHashMap<>();
        /** Pins a no-connect mark stands on: nothing joined to them is an error. */
        private final List<String> excused = new ArrayList<>();
        /** Every connection point of every sheet, by its key. */
        private final Set<String> points = new LinkedHashSet<>();

        private record Naming(int strength, String name) {
        }

        private static final int GLOBAL = 0;
        private static final int HIERARCHICAL = 1;
        private static final int LOCAL = 2;

        void sheet(SheetInstance instance) {
            KiCadSchematic schematic = instance.schematic();
            String sheet = instance.path();
            List<String> pointKeys = new ArrayList<>();
            List<KiCadPoint> points = new ArrayList<>();
            // Every connection point of the sheet, each a key of its own place on it.
            for (KiCadSymbolInstance symbol : schematic.symbols()) {
                KiCadSymbolDefinition definition = schematic.definitionOf(symbol).orElse(null);
                if (definition == null) {
                    continue;
                }
                String reference = symbol.reference(sheet);
                for (KiCadPin pin : definition.pinsOf(symbol.unit())) {
                    KiCadPoint at = symbol.placement().place(pin.at());
                    String pinKey = "pin:" + sheet + ":" + symbol.uuid() + ":" + pin.number();
                    if (definition.power()) {
                        // A power symbol's pin is its net's name across the design, never a pin of the net.
                        name(pinKey, GLOBAL, symbol.value());
                        union(pinKey, "global:" + symbol.value());
                    } else {
                        pins.put(pinKey, new KiCadConnection(sheet, reference, unitLetter(definition, symbol), symbol, pin, at));
                        if (pin.joinsByName()) {
                            name(pinKey, GLOBAL, pin.name());
                            union(pinKey, "global:" + pin.name());
                        }
                    }
                    String point = point(sheet, at);
                    union(pinKey, point);
                    pointKeys.add(point);
                    points.add(at);
                }
            }
            for (KiCadLabel label : schematic.labels()) {
                String point = point(sheet, label.at());
                pointKeys.add(point);
                points.add(label.at());
                switch (label.kind()) {
                    case GLOBAL -> {
                        name(point, GLOBAL, label.text());
                        union(point, "global:" + label.text());
                    }
                    case HIERARCHICAL -> {
                        name(point, HIERARCHICAL, displayPath(instance) + label.text());
                        union(point, "hierarchical:" + instance.path() + ":" + label.text());
                    }
                    case LOCAL -> {
                        name(point, LOCAL, displayPath(instance) + label.text());
                        union(point, "local:" + sheet + ":" + label.text());
                    }
                }
            }
            // The pins of the boxes this sheet places: each joined to its child's hierarchical labels of its name.
            for (KiCadSheet box : schematic.sheets()) {
                for (KiCadSheet.Pin pin : box.pins()) {
                    String point = point(sheet, pin.at());
                    pointKeys.add(point);
                    points.add(pin.at());
                    union(point, "hierarchical:" + sheet + "/" + box.uuid() + ":" + pin.name());
                }
            }
            for (KiCadPoint junction : schematic.junctions()) {
                pointKeys.add(point(sheet, junction));
                points.add(junction);
            }
            for (KiCadPoint mark : schematic.noConnects()) {
                excused.add(point(sheet, mark));
            }
            // Every wire's corners are connection points too: one ending in the middle of another makes a T.
            for (KiCadWire wire : schematic.wires()) {
                for (KiCadPoint corner : wire.points()) {
                    pointKeys.add(point(sheet, corner));
                    points.add(corner);
                }
            }
            // A wire joins the ends of its segments, and every point that lies on one of them.
            for (KiCadWire wire : schematic.wires()) {
                for (int index = 1; index < wire.points().size(); index++) {
                    KiCadPoint start = wire.points().get(index - 1);
                    KiCadPoint end = wire.points().get(index);
                    String segment = point(sheet, start);
                    union(segment, point(sheet, end));
                    for (int point = 0; point < points.size(); point++) {
                        if (points.get(point).liesOn(start, end)) {
                            union(segment, pointKeys.get(point));
                        }
                    }
                }
            }
            this.points.addAll(pointKeys);
        }

        /** A unit's letter, {@code A} for unit 1, when the symbol has more than one; else empty. */
        private static String unitLetter(KiCadSymbolDefinition definition, KiCadSymbolInstance symbol) {
            boolean several = definition.pins().stream().mapToInt(KiCadPin::unit).max().orElse(0) > 1;
            return several ? String.valueOf((char) ('A' + symbol.unit() - 1)) : "";
        }

        /** How a sheet instance's path reads in a net's name: {@code /} at the root, {@code /power/} below it. */
        private static String displayPath(SheetInstance instance) {
            StringBuilder path = new StringBuilder("/");
            List<String> names = new ArrayList<>();
            for (SheetInstance above = instance; above != null && !above.root(); above = above.parent()) {
                names.addFirst(above.name());
            }
            names.forEach(name -> path.append(name).append('/'));
            return path.toString();
        }

        static String point(String sheet, KiCadPoint at) {
            return "point:" + sheet + ":" + at.key();
        }

        private void name(String key, int strength, String name) {
            names.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new Naming(strength, name));
        }

        private String find(String key) {
            String parent = parents.getOrDefault(key, key);
            if (parent.equals(key)) {
                return key;
            }
            String root = find(parent);
            parents.put(key, root);
            return root;
        }

        private void union(String one, String other) {
            String first = find(one);
            String second = find(other);
            if (!first.equals(second)) {
                parents.put(second, first);
            }
        }

        KiCadConnectivity result() {
            Map<String, List<KiCadConnection>> groups = new LinkedHashMap<>();
            Map<String, Naming> groupNames = new HashMap<>();
            pins.forEach((key, pin) -> groups.computeIfAbsent(find(key), ignored -> new ArrayList<>()).add(pin));
            names.forEach((key, namings) -> namings.forEach(naming -> groupNames.merge(find(key), naming,
                    (kept, offered) -> offered.strength() < kept.strength()
                            || (offered.strength() == kept.strength() && offered.name().compareTo(kept.name()) < 0) ? offered : kept)));
            Map<String, KiCadNet> byName = new TreeMap<>();
            Map<String, String> groupNets = new HashMap<>();
            List<KiCadConnection> unconnected = new ArrayList<>();
            List<String> excusedGroups = excused.stream().map(this::find).toList();
            groups.forEach((group, members) -> {
                members.sort(KiCadNetNames.PIN_ORDER);
                Naming naming = groupNames.get(group);
                boolean alone = members.size() == 1 && naming == null;
                if (alone && !excusedGroups.contains(group)) {
                    unconnected.add(members.getFirst());
                }
                String name = naming != null ? naming.name() : KiCadNetNames.nameOf(members);
                groupNets.put(group, name);
                byName.merge(name, new KiCadNet(name, members), (one, other) -> {
                    List<KiCadConnection> both = new ArrayList<>(one.pins());
                    both.addAll(other.pins());
                    return new KiCadNet(name, both);
                });
            });
            Map<String, String> pointNets = new HashMap<>();
            for (String point : points) {
                String group = find(point);
                Naming naming = groupNames.get(group);
                String name = groupNets.getOrDefault(group, naming == null ? null : naming.name());
                if (name != null) {
                    pointNets.put(point, name);
                }
            }
            return new KiCadConnectivity(List.copyOf(byName.values()), unconnected, pointNets);
        }
    }
}
