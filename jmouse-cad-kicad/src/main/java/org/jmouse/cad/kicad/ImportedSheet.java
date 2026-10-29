package org.jmouse.cad.kicad;

import org.jmouse.kicad.symbol.KiCadSymbolDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One KiCad sheet as an import reads it, before it is a circuit's document: its parts, where each is drawn, its wires,
 * the probes it opens on, and what it hands across its edge — what {@link KiCadCircuitDocument} writes.
 *
 * @param ports      the nets the sheet hands to the block that places it: its hierarchical labels
 * @param globals    the nets every sheet of the project shares by name: its global labels and power rails
 * @param symbols    the units of KiCad symbols it draws with KiCad's own drawing
 * @param components the KiCad symbols its parts are made from, where the library map names no component
 * @param notes      what it could not carry, by part
 * @param footprints each part's KiCad footprint, by the part's reference: the library id its {@code Footprint} names
 */
public record ImportedSheet(List<Part> parts, List<Item> items, List<Wire> wires, List<Probe> probes, List<String> ports,
                            Set<String> globals, Set<SymbolUse> symbols, Set<KiCadSymbolDefinition> components,
                            List<String> notes, Map<String, String> footprints) {

    public ImportedSheet {
        parts = List.copyOf(parts);
        items = List.copyOf(items);
        wires = List.copyOf(wires);
        probes = List.copyOf(probes);
        ports = List.copyOf(ports);
        globals = Collections.unmodifiableSet(new LinkedHashSet<>(globals));
        symbols = Collections.unmodifiableSet(new LinkedHashSet<>(symbols));
        components = Collections.unmodifiableSet(new LinkedHashSet<>(components));
        notes = List.copyOf(notes);
        footprints = Collections.unmodifiableMap(new LinkedHashMap<>(footprints));
    }

    /**
     * A part: its component — a catalogue's, one made from KiCad's symbol, or the circuit of a sheet it places —, each
     * pin's net, what it sets, and the symbol and footprint it names beside its component's own.
     *
     * @param overrides each parameter it sets, as a number
     * @param symbol    the symbol it is drawn with when not its component's first, or null
     * @param footprint the footprint it sits on when not its component's own, or null
     */
    public record Part(String id, String component, Map<String, String> pins, Map<String, String> overrides, String symbol,
                       String footprint) {

        public Part {
            pins = Collections.unmodifiableMap(new LinkedHashMap<>(pins));
            overrides = Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
        }

        Part onFootprint(String slug) {
            return new Part(id, component, pins, overrides, symbol, slug);
        }
    }

    /**
     * Where something is drawn: a part — or one block of it, {@code unit} —, a sheet's box of {@code size}, or a mark.
     *
     * @param mark what a mark is — {@code ground}, {@code label}, {@code port}, {@code global}, {@code junction} —, or null
     *             for a part
     * @param net  the net a mark names, or null
     * @param size a placed sheet's box, width and height; null for anything else
     */
    public record Item(String id, String mark, String net, double x, double y, double rotation, boolean mirrored,
                       String unit, double[] size) {

        Item(String id, String mark, String net, double x, double y, double rotation, boolean mirrored) {
            this(id, mark, net, x, y, rotation, mirrored, null, null);
        }

        Item(String id, String mark, String net, double x, double y, double rotation, boolean mirrored, String unit) {
            this(id, mark, net, x, y, rotation, mirrored, unit, null);
        }
    }

    /** A wire from one terminal to another — {@code R1.positive} —, through its corners. */
    public record Wire(String id, String from, String to, List<double[]> bends) {

        public Wire {
            bends = List.copyOf(bends);
        }
    }

    /** An oscilloscope channel on a net's voltage, at {@code scale} volts a division. */
    public record Probe(String channel, String net, double scale) {
    }

    /** A unit of a KiCad symbol the sheet draws with KiCad's own drawing, its pins named as the part names them. */
    public record SymbolUse(KiCadSymbolDefinition symbol, int unit, Map<String, String> pinNames) {
    }
}
