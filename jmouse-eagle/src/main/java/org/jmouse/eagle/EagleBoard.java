package org.jmouse.eagle;

import java.util.List;
import java.util.Optional;

/**
 * A board: the libraries it carries, what is drawn on it beside its parts — its outline on layer 20 among it —, its
 * elements, and its signals, each with the pads it joins and the copper that joins them.
 */
public record EagleBoard(List<EagleLibrary> libraries, List<EagleGraphic> plain, List<Element> elements, List<Signal> signals) {

    /** Eagle's layer of the board's outline. */
    public static final int DIMENSION = 20;
    public static final int TOP = 1;
    public static final int BOTTOM = 16;

    public EagleBoard {
        libraries = List.copyOf(libraries);
        plain = List.copyOf(plain);
        elements = List.copyOf(elements);
        signals = List.copyOf(signals);
    }

    public Optional<EagleLibrary> library(String name) {
        return libraries.stream().filter(library -> library.name().equals(name)).findFirst();
    }

    /** A part placed on the board: its package, where it stands, and how it is turned — mirrored, on the bottom. */
    public record Element(String name, String library, String packageName, String value, double x, double y, EagleRotation rotation) {
    }

    /** A signal: the pads it joins, and its tracks, vias and poured areas. */
    public record Signal(String name, List<ContactReference> contacts, List<EagleGraphic.Wire> wires, List<Via> vias,
                         List<EagleGraphic.Polygon> polygons) {

        public Signal {
            contacts = List.copyOf(contacts);
            wires = List.copyOf(wires);
            vias = List.copyOf(vias);
            polygons = List.copyOf(polygons);
        }
    }

    /** A pad of an element. */
    public record ContactReference(String element, String pad) {
    }

    /**
     * A via: its drill, its ring's diameter — zero for one the design rules size —, and the layers it joins.
     *
     * @param extent the layers it runs between, as Eagle writes them: {@code 1-16}
     */
    public record Via(double x, double y, double drill, double diameter, String extent) {
    }
}
