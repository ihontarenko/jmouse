package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.simulation.CatalogueDocuments;

import java.util.List;
import java.util.Map;

/**
 * One placed sheet as it runs inside a circuit: what an editor opening a block shows — the sheet as it is drawn — and how
 * the circuit's live run names what is in it, so the drawing can show the run's figures.
 *
 * @param block the block as the run names it: {@code S1}, one deeper {@code S1_S2}
 * @param sheet the sheet as written, its own names
 * @param parts each part of the sheet, by its own reference → the run's: {@code R1} → {@code S1_R1}
 * @param nets  each net of the sheet, by its own name → the run's: a port the parent's net, a global itself, any other
 *              {@code S1.mid}
 * @param values each parameter of the sheet as this block runs it, in SI: its own value, or what the block sets
 */
public record SheetInstance(String block, Circuit sheet, Map<String, String> parts, Map<String, String> nets,
                            Map<String, Double> values) {

    public SheetInstance {
        parts = Map.copyOf(parts);
        nets = Map.copyOf(nets);
        values = Map.copyOf(values);
    }

    /**
     * The block a path of references reaches from a circuit, outermost first.
     *
     * @param values the circuit's parameter values, which a block's settings are written over
     * @throws IllegalArgumentException when a reference of the path is no block of the sheet it is looked for in
     */
    public static SheetInstance of(Circuit circuit, Map<String, Double> values, CatalogueDocuments catalogue,
                                   List<String> path) {
        return SheetHierarchy.placed(circuit, values, catalogue, path);
    }
}
