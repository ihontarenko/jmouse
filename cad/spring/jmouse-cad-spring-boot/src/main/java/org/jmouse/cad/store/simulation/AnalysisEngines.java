package org.jmouse.cad.store.simulation;

import org.jmouse.cad.document.DocumentInput;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.library.CadLanguage;
import org.jmouse.cad.simulation.AnalysisEngine;
import org.jmouse.cad.simulation.EngineTransient;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.cad.store.simulation.SimulationViews.Transient;
import org.jmouse.el.grammar.GrammarRefusal;
import org.jmouse.plugin.Extensions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * The engines the plugins brought beside the engine's own — {@code JMOUSE_CAD_API.md} §2.4: a circuit's transient
 * computed by another solver, answered as the engine's own transient is, so a bench compares the two line by line.
 */
public class AnalysisEngines {

    /** An engine as a bench offers it; one not {@code available} is listed so the bench can say why it is off. */
    public record Engine(String id, String title, boolean available) {
    }

    /**
     * @param result what it computed, as the engine's own transient is answered: each net's voltage, each source's
     *               current; the peaks, powers and logic levels are the engine's alone
     * @param given  what the engine ran, in its own words
     */
    public record EngineRun(String engine, Transient result, String given, List<String> notes) {
    }

    private final Extensions   extensions;
    private final CadCatalogue catalogue;
    private final CadAuthority authority;
    private final CadPlaces    places;
    private final CadLanguage  language;
    private final int          defaultSamples;
    private final int          maximumSamples;

    public AnalysisEngines(Extensions extensions, CadCatalogue catalogue, CadAuthority authority, CadPlaces places,
                           CadLanguage language, CadProperties properties) {
        this.extensions = extensions;
        this.catalogue = catalogue;
        this.authority = authority;
        this.places = places;
        this.language = language;
        this.defaultSamples = properties.simulation().engineSamples();
        this.maximumSamples = properties.simulation().maximumSamples();
    }

    public List<Engine> engines() {
        return extensions.contributions(AnalysisEngine.POINT).stream()
                .map(engine -> new Engine(engine.id(), engine.title(), engine.available())).toList();
    }

    /**
     * @param parameters its parameters at other values than the document's — where a bench's sliders stand; may be null
     * @param samples    how many even steps it is printed in; null for this server's own
     * @param owner      whose components its parts may also name, and where it is run; null for the catalogue's
     */
    @Transactional(readOnly = true)
    public EngineRun runTransient(String engineId, String source, Map<String, Double> parameters, Integer samples,
                                  DocumentOwner owner) {
        AnalysisEngine engine = extensions.contributions(AnalysisEngine.POINT).stream()
                .filter(candidate -> candidate.id().equals(engineId)).findFirst()
                .orElseThrow(() -> CadProblem.notFound("The engine '" + engineId + "'"));
        if (!engine.available()) {
            throw new CadProblem.Unprocessable("engine-unavailable", engine.title() + " does not run on this server.");
        }
        int printed = samples == null ? defaultSamples : samples;
        if (printed < 2 || printed > maximumSamples) {
            throw CadProblem.invalid("A run is printed in 2 to " + maximumSamples + " samples, not " + printed + ".");
        }
        authority.require(CadCapability.SIMULATION_RUN, owner == null ? CadPlace.installation() : places.requireOwner(owner));
        StoredDocuments scope = scope(owner);
        DocumentNode circuit = read(source);
        if (!circuit.kind().equals(SimulationModule.CIRCUIT)) {
            throw CadProblem.invalid("An engine runs a circuit; this text holds a " + circuit.kind().word() + ".");
        }
        EngineTransient run;
        try {
            run = engine.runTransient(circuit, scope, parameters == null ? Map.of() : parameters, printed);
        } catch (IllegalArgumentException refused) {
            throw new CadProblem.Unprocessable("conversion-refused", refused.getMessage());
        } catch (IllegalStateException failed) {
            throw new CadProblem.Unprocessable("engine-failed", engine.title() + " could not compute it: " + failed.getMessage());
        }
        Transient result = new Transient(SimulationVocabulary.TRANSIENT, circuit.slug(), run.time(), run.voltages(),
                                         Map.of(), Map.of(), run.currents(), Map.of(), List.of());
        return new EngineRun(engine.id(), result, run.given(), run.notes());
    }

    private StoredDocuments scope(DocumentOwner owner) {
        if (owner == null) {
            return catalogue.scope(null);
        }
        authority.require(CadCapability.OWNER_READ, places.requireOwner(owner));
        return catalogue.scope(owner);
    }

    private DocumentNode read(String source) {
        if (source == null || source.isBlank()) {
            throw CadProblem.invalid("A circuit is its jM text: send it as 'source'.");
        }
        try {
            return language.language().readOne(DocumentInput.text("the document", source));
        } catch (GrammarRefusal refused) {
            throw CadProblem.invalidDocument(refused.detail(), refused.line(), refused.column());
        }
    }
}
