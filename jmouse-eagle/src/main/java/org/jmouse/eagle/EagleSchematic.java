package org.jmouse.eagle;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A schematic: the libraries it carries, its parts, and its sheets, each with its instances and its nets. */
public record EagleSchematic(List<EagleLibrary> libraries, List<Part> parts, List<Sheet> sheets) {

    public EagleSchematic {
        libraries = List.copyOf(libraries);
        parts = List.copyOf(parts);
        sheets = List.copyOf(sheets);
    }

    public Optional<EagleLibrary> library(String name) {
        return libraries.stream().filter(library -> library.name().equals(name)).findFirst();
    }

    public Optional<Part> part(String name) {
        return parts.stream().filter(part -> part.name().equals(name)).findFirst();
    }

    /**
     * A part: the device set and device it is, its technology, its value where it has one, and its own attributes.
     *
     * @param value what its author wrote as its value; null for none
     */
    public record Part(String name, String library, String deviceSet, String device, String technology, String value,
                       Map<String, String> attributes) {

        public Part {
            attributes = EagleLibrary.ordered(attributes);
        }
    }

    /** A sheet: where each gate of each part is drawn, its nets, and what else is drawn on it. */
    public record Sheet(List<Instance> instances, List<Bus> buses, List<Net> nets, List<EagleGraphic> plain) {

        public Sheet {
            instances = List.copyOf(instances);
            buses = List.copyOf(buses);
            nets = List.copyOf(nets);
            plain = List.copyOf(plain);
        }
    }

    /** A bus: its name, which says the nets it carries — {@code D[0..7]} —, and the wires it is drawn with. */
    public record Bus(String name, List<EagleGraphic.Wire> wires) {

        public Bus {
            wires = List.copyOf(wires);
        }
    }

    /** Where a gate of a part is drawn, and how it is turned. */
    public record Instance(String part, String gate, double x, double y, EagleRotation rotation) {
    }

    /** A net: its segments, each a set of pins joined by wires. */
    public record Net(String name, List<Segment> segments) {

        public Net {
            segments = List.copyOf(segments);
        }
    }

    /** A connected piece of a net: the pins it touches, its wires, its junction dots and the labels naming it. */
    public record Segment(List<PinReference> pins, List<EagleGraphic.Wire> wires, List<Point> junctions, List<Point> labels) {

        public Segment {
            pins = List.copyOf(pins);
            wires = List.copyOf(wires);
            junctions = List.copyOf(junctions);
            labels = List.copyOf(labels);
        }
    }

    /** A pin of a gate of a part. */
    public record PinReference(String part, String gate, String pin) {
    }

    public record Point(double x, double y) {
    }
}
