package org.jmouse.cad.spice;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.AnalysisEngine;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.EngineTransient;

import java.util.Map;

/**
 * A SPICE program as an engine beside the engine's own: the circuit written as a deck closed in the program's dialect,
 * run in batch, its printout read back in the circuit's names. Listed while its plugin's settings name no program, so a
 * bench can say why it is off. A plugin brings one per program — ngspice, Xyce — with the program's dialect.
 */
public final class SpiceEngine implements AnalysisEngine {

    private final String         id;
    private final SpiceDialect   dialect;
    private final SpiceSimulator simulator;

    /**
     * @param id        the engine's word on the routes: {@code ngspice}
     * @param simulator the program as a process, off while it says it is not set
     */
    public SpiceEngine(String id, SpiceDialect dialect, SpiceSimulator simulator) {
        this.id = id;
        this.dialect = dialect;
        this.simulator = simulator;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return dialect.program();
    }

    @Override
    public boolean available() {
        return simulator.isSet();
    }

    @Override
    public EngineTransient runTransient(DocumentNode circuit, CatalogueDocuments catalogue, Map<String, Double> parameters,
                                        int samples) {
        if (!simulator.isSet()) {
            throw new IllegalStateException("No program is set to run " + dialect.program() + ": set it in the plugin's settings.");
        }
        try {
            SpiceTransient.Run run = new SpiceTransient(catalogue, simulator, dialect).run(circuit, parameters, samples);
            return new EngineTransient(run.time(), run.voltages(), run.currents(), run.deck(), run.notes());
        } catch (SpiceRunException failed) {
            throw new IllegalStateException(failed.getMessage(), failed);
        }
    }
}
