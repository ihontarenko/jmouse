package org.jmouse.parallax.kicad;

import org.jmouse.parallax.object.ComponentLibrary;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What an import asks of the catalogue about a component a KiCad part is: which of its pins a package's number is, and
 * how it is drawn — its default symbol and where each of its pins stands on it —, so the part is drawn with the
 * catalogue's own living symbol, set where KiCad's pins stand. Whoever holds the catalogue answers: the store, or a
 * folder of documents.
 */
public interface KiCadCatalogue {

    /** A catalogue that knows nothing: every part keeps KiCad's symbol, every pin needs the library map. */
    KiCadCatalogue NONE = new KiCadCatalogue() {
        @Override
        public Optional<PartDrawing> drawingOf(String component) {
            return Optional.empty();
        }

        @Override
        public Map<String, String> pinsByNumber(String component) {
            return Map.of();
        }
    };

    /** The catalogue a component library holds: the documents it resolves by slug, components and symbols alike. */
    static KiCadCatalogue of(ComponentLibrary library) {
        return new LibraryCatalogue(library);
    }

    /**
     * @param symbol the symbol document's slug, drawn by default for the component
     * @param pins   each of the component's pins, by name, at its point in the symbol's drawing units
     */
    record PartDrawing(String symbol, Map<String, double[]> pins) {

        public PartDrawing {
            pins = Map.copyOf(pins);
        }
    }

    /** How a component is drawn; empty when the catalogue does not draw it. */
    Optional<PartDrawing> drawingOf(String component);

    /**
     * Every symbol the component may be drawn with, its default first — the symbols its {@code symbol uses} line names,
     * each with the same port map. An import draws a part with the one whose pins stand most where KiCad's do.
     */
    default List<PartDrawing> drawingsOf(String component) {
        return drawingOf(component).map(List::of).orElse(List.of());
    }

    /**
     * The component's pin by its package's number — {@code 1} is {@code anode}; on a part of several blocks the
     * block's pin, {@code 7} is {@code B.output} —; empty when the component numbers none.
     */
    Map<String, String> pinsByNumber(String component);

    /** The footprint a component's own {@code footprint} line names; empty when it names none. */
    default Optional<String> footprintOf(String component) {
        return Optional.empty();
    }
}
