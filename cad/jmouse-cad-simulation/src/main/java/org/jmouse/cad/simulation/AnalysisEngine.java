package org.jmouse.cad.simulation;

import org.jmouse.cad.document.DocumentNode;
import org.jmouse.plugin.Contributions;
import org.jmouse.plugin.Identified;

import java.util.Map;

/**
 * A solver beside the engine's own, for a circuit's batch analyses: ngspice. The bench offers a choice of engines only
 * when one is here and {@link #available()}.
 */
public interface AnalysisEngine extends Identified {

    Contributions<AnalysisEngine> POINT = new Contributions<>("cad.analysis.engines", AnalysisEngine.class);

    String title();

    /** Whether it runs on this server now: the program it drives may not be installed. */
    boolean available();

    /**
     * A circuit's transient, printed at even steps.
     *
     * @param catalogue  the components its parts name, as the reader may see them
     * @param parameters its parameters at other values than the document's; empty for its own
     * @throws IllegalArgumentException when the circuit cannot be said to this engine, with what
     * @throws IllegalStateException    when the engine ran and failed, with what it said
     */
    EngineTransient runTransient(DocumentNode circuit, CatalogueDocuments catalogue, Map<String, Double> parameters,
                                 int samples);
}
