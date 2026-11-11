package org.jmouse.cad.spice;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.simulation.AnalysisEngine;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.EngineTransient;

import java.util.Map;
import java.util.Optional;

/**
 * A SPICE program as an engine beside the engine's own: the circuit written as a deck closed in the program's dialect,
 * run in batch, its printout read back in the circuit's names. Listed when the server names no program, so a bench can
 * say why it is off. A plugin brings one per program — ngspice, Xyce — with the program's dialect.
 */
public final class SpiceEngine implements AnalysisEngine {

    private final String                   id;
    private final SpiceDialect             dialect;
    private final Optional<SpiceSimulator> simulator;
    private final String                   setting;

    /**
     * @param id        the engine's word on the routes: {@code ngspice}
     * @param simulator the program as a process; empty for a server without it
     * @param setting   the property that names the program, said when it is off: {@code jmouse.cad.spice.command}
     */
    public SpiceEngine(String id, SpiceDialect dialect, Optional<SpiceSimulator> simulator, String setting) {
        this.id = id;
        this.dialect = dialect;
        this.simulator = simulator;
        this.setting = setting;
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
        return simulator.isPresent();
    }

    @Override
    public EngineTransient runTransient(DocumentNode circuit, CatalogueDocuments catalogue, Map<String, Double> parameters,
                                        int samples) {
        SpiceSimulator program = simulator.orElseThrow(() -> new IllegalStateException(
                setting + " names no program that runs " + dialect.program()));
        try {
            SpiceTransient.Run run = new SpiceTransient(catalogue, program, dialect).run(circuit, parameters, samples);
            return new EngineTransient(run.time(), run.voltages(), run.currents(), run.deck(), run.notes());
        } catch (SpiceRunException failed) {
            throw new IllegalStateException(failed.getMessage(), failed);
        }
    }
}
