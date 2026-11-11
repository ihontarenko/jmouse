package org.jmouse.cad.kicad;

import org.jmouse.cad.simulation.CatalogueDocuments;

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

    /** The catalogue a set of documents is, by slug: components and symbols alike. */
    static KiCadCatalogue of(CatalogueDocuments documents) {
        return new DocumentCatalogue(documents);
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

    /**
     * The names of the component's parameters, in its order: what a KiCad symbol's field of the same name sets — a
     * switch's {@code closed} —, where every other field of a design (a part number, a maker) is not ours to read.
     */
    default List<String> parametersOf(String component) {
        return List.of();
    }

    /** The parameter the component's value is — what KiCad's {@code Value} field holds —; empty when it names none. */
    default Optional<String> valueParameterOf(String component) {
        return Optional.empty();
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

    /** A catalogue symbol by its slug and where each of its pins stands; empty when the catalogue does not hold it. */
    default Optional<PartDrawing> symbolDrawing(String symbol) {
        return Optional.empty();
    }

    /** The footprint a component's own {@code footprint} line names; empty when it names none. */
    default Optional<String> footprintOf(String component) {
        return Optional.empty();
    }

    /**
     * The footprint whose text writes this code — {@code meta { code "FP.CHIP:R0603" }} —: what the library map pairs a
     * KiCad footprint with. Empty where the catalogue keeps no index of codes, and the import then converts KiCad's.
     */
    default Optional<String> footprintCoded(String code) {
        return Optional.empty();
    }

    /**
     * The symbol whose text writes this code — {@code meta { code "SY.DIODE:RECTIFIER" }} —: what the library map draws a
     * KiCad symbol with no model as. Empty where the catalogue keeps no index of codes, and the part keeps KiCad's drawing.
     */
    default Optional<String> symbolCoded(String code) {
        return Optional.empty();
    }
}
