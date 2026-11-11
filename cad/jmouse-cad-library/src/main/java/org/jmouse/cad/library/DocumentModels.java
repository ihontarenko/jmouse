package org.jmouse.cad.library;

import org.jmouse.cad.board.BoardModule;
import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.schematic.SchematicModule;
import org.jmouse.cad.schematic.drawing.LivingDrawing;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationReading;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * What a document is to the engine, read from its tree: a {@code Component}, a {@code Family}, a {@code Circuit}, a
 * {@code SymbolDrawing} for a symbol, a portrait or a frame, a {@code FootprintDrawing}, a {@code BoardDrawing}. A kind
 * the engine keeps no model of — a function library, a category — has none.
 *
 * <p>Reading a model is where a document that reads as text is refused for what it says: a pin no wire reaches, a
 * drawing line the engine cannot draw.</p>
 */
public final class DocumentModels {

    private static final Map<String, Function<DocumentNode, Object>> READERS = Map.of(
            SimulationModule.COMPONENT.word(), SimulationReading::component,
            SimulationModule.FAMILY.word(), SimulationReading::family,
            SimulationModule.CIRCUIT.word(), SimulationReading::circuit,
            SchematicModule.SYMBOL.word(), LivingDrawing::of,
            SchematicModule.PORTRAIT.word(), LivingDrawing::of,
            SchematicModule.FRAME.word(), LivingDrawing::of,
            BoardModule.FOOTPRINT.word(), FootprintDrawing::of,
            BoardModule.PCB.word(), BoardDrawing::of);

    private DocumentModels() {
    }

    /** The document's model, or empty for a kind the engine keeps none of. */
    public static Optional<Object> of(DocumentNode document) {
        Function<DocumentNode, Object> reader = READERS.get(document.kind().word());
        if (reader == null) {
            return Optional.empty();
        }
        return Optional.of(reader.apply(document));
    }
}
