package org.jmouse.cad.simulation;

import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.component.Family;
import org.jmouse.cad.document.DocumentKind;
import org.jmouse.cad.document.DocumentNode;

/**
 * Reads a simulated document's tree into the model: a circuit, a component, a family or a library of functions. The
 * tree is read once, by the document language; this only says what its nodes mean.
 */
public final class SimulationReading {

    private SimulationReading() {
    }

    public static Circuit circuit(DocumentNode document) {
        require(document, SimulationModule.CIRCUIT);
        return CircuitReading.circuit(document);
    }

    public static Component component(DocumentNode document) {
        require(document, SimulationModule.COMPONENT);
        return ComponentReading.component(document);
    }

    /** A family — what its members share — or a library of functions: one model, as both write the same blocks. */
    public static Family family(DocumentNode document) {
        if (!document.kind().equals(SimulationModule.FAMILY) && !document.kind().equals(SimulationModule.FUNCTIONS)) {
            throw new IllegalArgumentException("'" + document.slug() + "' is a " + document.kind().word() + ", not a family "
                    + "or a library of functions");
        }
        return ComponentReading.family(document);
    }

    private static void require(DocumentNode document, DocumentKind kind) {
        if (!document.kind().equals(kind)) {
            throw new IllegalArgumentException("'" + document.slug() + "' is a " + document.kind().word() + ", not a " + kind.word());
        }
    }
}
