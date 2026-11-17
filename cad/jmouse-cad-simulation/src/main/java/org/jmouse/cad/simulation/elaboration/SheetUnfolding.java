package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.ParameterEvaluation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A design as one flat circuit: every placed sheet replaced by its parts, however deep, named as a bench runs them —
 * {@code S1_R1}, a net {@code S1.mid} — so whoever reads a design whole sees the one set of parts and nets a bench does: a
 * pcb's parts stand on it. The rules of the naming are {@link SheetHierarchy}'s.
 */
public final class SheetUnfolding {

    private SheetUnfolding() {
    }

    /**
     * @throws IllegalArgumentException when a sheet places itself, or a block does not fit the sheet it places
     */
    public static Circuit of(Circuit circuit, CatalogueDocuments catalogue) {
        return SheetHierarchy.unfold(circuit, valuesOf(circuit), catalogue).circuit();
    }

    /**
     * What a part is called in the unfolded design: {@code S1_R1} for {@code R1} of a sheet placed as block {@code S1}.
     *
     * @param blocks the blocks it is placed through, outermost first; none for a part of the top sheet
     */
    public static String partName(List<String> blocks, String part) {
        if (blocks.isEmpty()) {
            return part;
        }
        return String.join(SheetHierarchy.PART_SEPARATOR, blocks) + SheetHierarchy.PART_SEPARATOR + part;
    }

    /**
     * What a net of a placed sheet is called in the unfolded design: {@code S1.mid} for {@code mid} of block {@code S1}, one
     * deeper {@code S1_S2.mid} — a net the sheet keeps its own; a port is the parent's net, a global net and node 0 keep their names.
     *
     * @param blocks the blocks it is placed through, outermost first; none for a net of the top sheet
     */
    public static String netName(List<String> blocks, String net) {
        if (blocks.isEmpty()) {
            return net;
        }
        return String.join(SheetHierarchy.PART_SEPARATOR, blocks) + ElaboratedCircuit.UNIT_SEPARATOR + net;
    }

    /**
     * Each port of the sheet a block wires — by name, or in the order the sheet writes its ports — and the parent's net it is.
     *
     * @throws IllegalArgumentException when the block wires a port its sheet has not
     */
    public static Map<String, String> portsOf(Part block, Circuit sheet) {
        return SheetHierarchy.pinsOf(block, sheet);
    }

    /**
     * A block as it is run: each pin carrying a bus bound member by member to the bus of the parent it names.
     *
     * @param parent the circuit the block stands in
     * @throws IllegalArgumentException when a bus pin is bound to no bus of the parent, or to one of another size
     */
    public static Part busesBound(Part block, Circuit parent, CatalogueDocuments catalogue) {
        return SheetHierarchy.busesUnfolded(block, parent, catalogue);
    }

    /** A circuit's parameters, each worked out from what it is written as. */
    public static Map<String, Double> valuesOf(Circuit circuit) {
        Map<String, Double> values = new LinkedHashMap<>();
        ParameterEvaluation.evaluate(circuit.head().title(), circuit.parameters())
                .forEach(parameter -> values.put(parameter.name(), parameter.value()));
        return values;
    }
}
