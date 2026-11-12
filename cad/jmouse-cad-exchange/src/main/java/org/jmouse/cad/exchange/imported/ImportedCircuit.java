package org.jmouse.cad.exchange.imported;

import org.jmouse.cad.circuit.Placement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One sheet of another product's design as an import reads it, before it is a circuit's document: its parts, where
 * each is drawn, its wires, the probes it opens on, and what it hands across its edge — what
 * {@link ImportedCircuitDocument} writes. Every format's import reads its sheets into this.
 *
 * @param ports   the nets the sheet hands to the block that places it: its hierarchical labels
 * @param globals the nets every sheet of the design shares by name: its global labels and power rails
 * @param notes   what it could not carry, by part
 * @param buses   the bus lines drawn on it; they join nothing, its labels do
 */
public record ImportedCircuit(List<Part> parts, List<Item> items, List<Wire> wires, List<Probe> probes, List<String> ports,
                              Set<String> globals, List<String> notes, List<Bus> buses) {

    /** A sheet with no bus line. */
    public ImportedCircuit(List<Part> parts, List<Item> items, List<Wire> wires, List<Probe> probes, List<String> ports,
                           Set<String> globals, List<String> notes) {
        this(parts, items, wires, probes, ports, globals, notes, List.of());
    }

    /** What a part's or a mark's id opens with where the product's reference opens with no letter: {@code 3V3} is {@code P3V3}. */
    public static final String ID_OPENING = "P";

    /** How long a part's or a mark's id is at most. */
    public static final int ID_LONGEST = 28;

    /** A coordinate of the sheet to a hundredth of a drawing unit: what is left below it is arithmetic, not drawing. */
    public static double rounded(double units) {
        return Math.round(units * 100) / 100.0;
    }

    /** The nearest point of the sheet's grid ({@link SymbolCanvas#GRID}). */
    public static double onGrid(double units) {
        return Math.round(units / SymbolCanvas.GRID) * SymbolCanvas.GRID;
    }

    public ImportedCircuit {
        parts = List.copyOf(parts);
        items = List.copyOf(items);
        wires = List.copyOf(wires);
        probes = List.copyOf(probes);
        ports = List.copyOf(ports);
        globals = Collections.unmodifiableSet(new LinkedHashSet<>(globals));
        notes = List.copyOf(notes);
        buses = List.copyOf(buses);
    }

    /** The same sheet with its parts as given: each on the footprint the import settled. */
    public ImportedCircuit withParts(List<Part> settled) {
        return new ImportedCircuit(settled, items, wires, probes, ports, globals, notes, buses);
    }

    /**
     * A part: its component — a catalogue's, one made from the product's symbol, or the circuit of a sheet it places —, each
     * pin's net, what it sets, and the symbol and footprint it names beside its component's own.
     *
     * @param overrides each parameter it sets, as a number
     * @param symbol    the symbol it is drawn with when not its component's first, or null
     * @param footprint the footprint it sits on when not its component's own, or null
     * @param value     its value as the design says it in words, where no parameter of its component takes it; or null
     */
    public record Part(String id, String component, Map<String, String> pins, Map<String, String> overrides, String symbol,
                       String footprint, Map<String, String> pads, String value) {

        public Part {
            pins = Collections.unmodifiableMap(new LinkedHashMap<>(pins));
            overrides = Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
            pads = Collections.unmodifiableMap(new LinkedHashMap<>(pads));
        }

        /** A part with no value in words. */
        public Part(String id, String component, Map<String, String> pins, Map<String, String> overrides, String symbol,
                    String footprint, Map<String, String> pads) {
            this(id, component, pins, overrides, symbol, footprint, pads, null);
        }

        /** A part on its component's pads. */
        public Part(String id, String component, Map<String, String> pins, Map<String, String> overrides, String symbol,
                    String footprint) {
            this(id, component, pins, overrides, symbol, footprint, Map.of(), null);
        }

        /** The same part with its value in words; a blank one is none. */
        public Part withValue(String words) {
            return new Part(id, component, pins, overrides, symbol, footprint, pads, words == null || words.isBlank() ? null : words.strip());
        }

        public Part onFootprint(String slug) {
            return new Part(id, component, pins, overrides, symbol, slug, pads, value);
        }

        /** On a footprint numbered its own way: {@code pads} says which pin each of its pads takes. */
        public Part onFootprint(String slug, Map<String, String> pads) {
            return new Part(id, component, pins, overrides, symbol, slug, pads, value);
        }
    }

    /**
     * Where something is drawn: a part — or one block of it, {@code unit} —, a sheet's box of {@code size}, or a mark.
     *
     * @param mark what a mark is — {@code ground}, {@code label}, {@code port}, {@code global}, {@code junction} —, or null
     *             for a part
     * @param net  the net a mark names, or null
     * @param size a placed sheet's box, width and height; null for anything else
     * @param name  where the design wrote the part's name, from the item's centre; null where the sheet is to place it
     * @param value where the design wrote the part's value, the same way
     */
    public record Item(String id, String mark, String net, double x, double y, double rotation, boolean mirrored,
                       String unit, double[] size, double[] name, double[] value, List<Placement.PortPlace> ports) {

        public Item {
            ports = ports == null ? List.of() : List.copyOf(ports);
        }

        public Item(String id, String mark, String net, double x, double y, double rotation, boolean mirrored) {
            this(id, mark, net, x, y, rotation, mirrored, null, null, null, null, List.of());
        }

        public Item(String id, String mark, String net, double x, double y, double rotation, boolean mirrored, String unit) {
            this(id, mark, net, x, y, rotation, mirrored, unit, null, null, null, List.of());
        }

        public Item(String id, String mark, String net, double x, double y, double rotation, boolean mirrored, String unit, double[] size) {
            this(id, mark, net, x, y, rotation, mirrored, unit, size, null, null, List.of());
        }

        /** The same item, its texts where the design wrote them. */
        public Item withTexts(double[] writtenName, double[] writtenValue) {
            return new Item(id, mark, net, x, y, rotation, mirrored, unit, size, writtenName, writtenValue, ports);
        }

        /** The same placed sheet, its ports standing where the design drew them on its frame. */
        public Item withPorts(List<Placement.PortPlace> placed) {
            return new Item(id, mark, net, x, y, rotation, mirrored, unit, size, name, value, placed);
        }
    }

    /** A wire from one terminal to another — {@code R1.positive} —, through its corners. */
    /** A bus line: what it is called, the nets it carries, and its corners. */
    public record Bus(String id, String name, List<String> members, List<double[]> via) {

        public Bus {
            members = List.copyOf(members);
            via = List.copyOf(via);
        }
    }

    public record Wire(String id, String from, String to, List<double[]> bends) {

        public Wire {
            bends = List.copyOf(bends);
        }
    }

    /** An oscilloscope channel on a net's voltage, at {@code scale} volts a division. */
    public record Probe(String channel, String net, double scale) {
    }
}
