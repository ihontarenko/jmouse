package org.jmouse.cad.store.catalogue;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.schematic.drawing.SymbolDrawing;
import org.jmouse.cad.simulation.ParameterSliders;

import java.util.List;

/**
 * What a document's model says of how it is shown, worked out once on the server so an interface only draws it: how
 * it is presented, and its parameters as sliders. A stored document and an unsaved text are told the same.
 */
public final class ModelFacts {

    /** A symbol, a portrait or a frame: drawn as it stands. */
    public static final String DRAWING    = "drawing";
    /** A component: a part, drawn from the symbols it names. */
    public static final String PART       = "part";
    /** A circuit: run on a bench, its instruments reading it. */
    public static final String INSTRUMENT = "instrument";
    /** A footprint: its pads and outlines. */
    public static final String FOOTPRINT  = "footprint";
    /** A printed circuit board: laid out in the board editor. */
    public static final String BOARD      = "board";

    private ModelFacts() {
    }

    /** How a document is presented, by what its model is; null for a model that is not shown — a category, a family. */
    public static String presentationOf(Object model) {
        return switch (model) {
            case SymbolDrawing ignored -> DRAWING;
            case Component ignored -> PART;
            case Circuit ignored -> INSTRUMENT;
            case FootprintDrawing ignored -> FOOTPRINT;
            case BoardDrawing ignored -> BOARD;
            case null, default -> null;
        };
    }

    /** A circuit's or a component's parameters as sliders; none for any other model, or one whose values do not compute. */
    public static List<ParameterSliders.Slider> slidersOf(Object model) {
        try {
            return switch (model) {
                case Circuit circuit -> ParameterSliders.of(circuit.head().title(), circuit.parameters());
                case Component component -> ParameterSliders.of(component.head().title(), component.parameters());
                case null, default -> List.of();
            };
        } catch (IllegalArgumentException uncomputed) {
            return List.of();
        }
    }
}
