package org.jmouse.cad.store.simulation;

import org.jmouse.cad.store.catalogue.WrittenOwners;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Setting;
import org.jmouse.cad.circuit.Analysis;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.jpa.catalogue.StoredDocument;
import org.jmouse.cad.simulation.SimulationModule;
import org.jmouse.cad.simulation.SimulationVocabulary;
import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;
import org.jmouse.cad.simulation.engine.CircuitSimulation;
import org.jmouse.cad.simulation.engine.Expectations;
import org.jmouse.cad.simulation.engine.MeasuredCircuit;
import org.jmouse.cad.simulation.engine.TransientRun;
import org.jmouse.cad.store.CadProperties;
import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.authority.CadPlaces;
import org.jmouse.cad.store.catalogue.CadCatalogue;
import org.jmouse.cad.store.catalogue.DocumentAccess;
import org.jmouse.cad.store.catalogue.DocumentReading;
import org.jmouse.cad.store.catalogue.ReadDocument;
import org.jmouse.cad.store.catalogue.StoredDocuments;
import org.jmouse.cad.store.problem.CadProblem;
import org.jmouse.cad.store.simulation.SimulationViews.ExpectationRequest;
import org.jmouse.cad.store.simulation.SimulationViews.Job;
import org.jmouse.cad.store.simulation.SimulationViews.OperatingPoint;
import org.jmouse.cad.store.simulation.SimulationViews.SimulationAnswer;
import org.jmouse.cad.store.simulation.SimulationViews.SimulationRequest;
import org.jmouse.cad.store.simulation.SimulationViews.SimulationResult;
import org.jmouse.cad.store.simulation.SimulationViews.Transient;
import org.jmouse.cad.store.simulation.SimulationViews.Verdict;
import org.jmouse.cad.store.simulation.SimulationViews.DocumentVerdicts;
import org.jmouse.numerics.ComputationProgress;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Running a circuit — {@code JMOUSE_CAD_API.md} §2.4: a transient or an operating point, answered at once when it is
 * small and queued as a job when it is not; and the expectations a document states of itself, measured.
 *
 * <p>The circuit is elaborated here, inside the request — its parts found in the catalogue, its values set as the request
 * asks through the engine's own {@link MeasuredCircuit} —; what a job is handed is that elaborated circuit alone.</p>
 */
public class Simulations {

    private final CadCatalogue    catalogue;
    private final DocumentAccess  access;
    private final DocumentStore   documents;
    private final DocumentReading reading;
    private final CadAuthority    authority;
    private final CadPlaces       places;
    private final SimulationJobs  jobs;
    private final CadProperties   properties;

    public Simulations(CadCatalogue catalogue, DocumentAccess access, DocumentStore documents, DocumentReading reading,
                       CadAuthority authority, CadPlaces places, SimulationJobs jobs, CadProperties properties) {
        this.catalogue = catalogue;
        this.access = access;
        this.documents = documents;
        this.reading = reading;
        this.authority = authority;
        this.places = places;
        this.jobs = jobs;
        this.properties = properties;
    }

    /** Runs a circuit: its result now, or a job when it asks for more samples than an answer holds. */
    @Transactional(readOnly = true)
    public SimulationAnswer simulate(SimulationRequest request) {
        DocumentOwner owner = WrittenOwners.optional(request.owner());
        String actor = requireRun(owner);
        StoredDocuments scope = catalogue.scope(owner);
        ReadDocument read = circuitText(request.circuit(), request.source(), scope);
        if (!(read.model().orElse(null) instanceof Circuit written)) {
            throw CadProblem.invalid("'" + read.index().slug() + "' is a " + read.index().kind() + ", not a circuit.");
        }
        String analysis = analysisOf(request, written);
        Circuit circuit = withDuration(written, analysis, request.duration());
        ElaboratedCircuit elaborated = elaborated(circuit, scope, request.parameters());
        String named = request.circuit() != null ? request.circuit() : read.index().slug();
        if (analysis.equals(SimulationVocabulary.OPERATING)) {
            return operatingPoint(named, elaborated);
        }
        int samples = samplesOf(request.samples());
        if (samples <= properties.simulation().inlineSamples()) {
            return transientRun(named, elaborated, samples, request.recordsEveryPart(), ComputationProgress.NONE);
        }
        return jobs.submit(actor, progress -> transientRun(named, elaborated, samples, request.recordsEveryPart(), progress));
    }

    /**
     * A featured circuit of the published catalogue run as its own text says, at a few samples — what a host shows a
     * visitor who is not signed in, beside the circuit's drawing. It needs no actor: the circuit is one everybody may read,
     * nothing of the run is chosen by the asker, and its cost is held to {@code samples} however long the circuit runs.
     *
     * @throws CadProblem.NotFound when the circuit is not a featured, published circuit of the catalogue
     */
    @Transactional(readOnly = true)
    public SimulationResult preview(String slug, int samples) {
        StoredDocument document = documents.findBySlug(slug)
                .filter(StoredDocument::isCatalogue).filter(StoredDocument::isFeatured)
                .filter(found -> found.getStatus() == StoredDocument.Status.PUBLISHED)
                .filter(found -> found.getKind().equals(SimulationModule.CIRCUIT.word()))
                .orElseThrow(() -> CadProblem.notFound("The featured circuit"));
        StoredDocuments scope = catalogue.scope(null);
        if (!(scope.head(document).model().orElse(null) instanceof Circuit written)) {
            throw CadProblem.notFound("The featured circuit");
        }
        String analysis = written.run().map(Analysis::kind).orElse(SimulationVocabulary.TRANSIENT);
        ElaboratedCircuit elaborated = elaborated(withDuration(written, analysis, null), scope, Map.of());
        if (analysis.equals(SimulationVocabulary.OPERATING)) {
            return operatingPoint(slug, elaborated);
        }
        return transientRun(slug, elaborated, samples, false, ComputationProgress.NONE);
    }

    public Job job(String id) {
        return jobs.job(id, authority.requireActor());
    }

    public SimulationResult result(String id) {
        return jobs.result(id, authority.requireActor());
    }

    public Job cancel(String id) {
        return jobs.cancel(id, authority.requireActor());
    }

    /** Every expectation a circuit or a component states of itself, measured on the nominal bench. */
    @Transactional(readOnly = true)
    public List<Verdict> expectations(ExpectationRequest request) {
        DocumentOwner owner = WrittenOwners.optional(request.owner());
        requireRun(owner);
        StoredDocuments scope = catalogue.scope(owner);
        ReadDocument read = circuitText(request.reference(), request.source(), scope);
        List<Expectations.Verdict> verdicts;
        try {
            Object model = read.model().orElse(null);
            if (model instanceof Circuit circuit) {
                verdicts = Expectations.ofCircuit(circuit, scope);
            } else if (model instanceof Component) {
                verdicts = Expectations.ofComponent(read.document(), scope);
            } else {
                throw CadProblem.invalid("Only a circuit or a component states expectations; '" + read.index().slug()
                                         + "' is a " + read.index().kind() + ".");
            }
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw new CadProblem.Unprocessable("simulation-failed", refused.getMessage());
        }
        return verdicts.stream().map(Simulations::verdictOf).toList();
    }

    /**
     * The published catalogue documents a catalogue check measures — every circuit and every component —, by slug. For
     * {@link CatalogueChecks}, which asked to run at the installation when it started.
     */
    @Transactional(readOnly = true)
    List<String> catalogueCheckable() {
        List<String> slugs = new ArrayList<>();
        for (String kind : List.of(SimulationModule.CIRCUIT.word(), SimulationModule.COMPONENT.word())) {
            for (StoredDocument document : documents.findByKind(kind)) {
                if (document.isCatalogue() && document.getStatus() == StoredDocument.Status.PUBLISHED) {
                    slugs.add(document.getSlug());
                }
            }
        }
        return slugs;
    }

    /** One catalogue document's expectations, measured; null for a document gone since the check began. */
    @Transactional(readOnly = true)
    DocumentVerdicts checkCatalogueDocument(String slug) {
        return documents.findBySlug(slug).map(document -> verdictsOf(document, catalogue.scope(null))).orElse(null);
    }

    private DocumentVerdicts verdictsOf(StoredDocument document, StoredDocuments scope) {
        try {
            ReadDocument read = scope.head(document);
            Object model = read.model().orElse(null);
            List<Expectations.Verdict> verdicts = model instanceof Circuit circuit ? Expectations.ofCircuit(circuit, scope)
                    : Expectations.ofComponent(read.document(), scope);
            return new DocumentVerdicts(document.getSlug(), document.getTitle(), document.getKind(),
                                        verdicts.stream().map(Simulations::verdictOf).toList(), null);
        } catch (CadProblem | IllegalArgumentException | IllegalStateException | ArithmeticException refused) {
            return new DocumentVerdicts(document.getSlug(), document.getTitle(), document.getKind(), List.of(),
                                        refused.getMessage());
        }
    }

    private static Verdict verdictOf(Expectations.Verdict verdict) {
        return new Verdict(verdict.title(), verdict.source(), verdict.measured().map(measured -> measured.written()).orElse(null),
                           verdict.window(), verdict.met(), verdict.refusal());
    }

    // ── The circuit ──────────────────────────────────────────────────────────────────────────────────────────────

    /** The text to run: a stored document the reader may see, by reference — or an unsaved one, sent as it is. */
    private ReadDocument circuitText(String reference, String source, StoredDocuments scope) {
        if ((reference == null) == (source == null)) {
            throw CadProblem.invalid("Name a stored document as 'circuit' or send its text as 'source' — one of the two.");
        }
        if (source != null) {
            return reading.read(source, "the circuit sent");
        }
        return scope.resolve(reference)
                .filter(read -> documents.findBySlug(read.index().slug())
                        .filter(document -> document.owner().isPresent() || access.canRead(document)).isPresent())
                .orElseThrow(() -> CadProblem.notFound("The document"));
    }

    /** The circuit as it runs: its parts found, its values and its parts' settings as the request sets them. */
    private static ElaboratedCircuit elaborated(Circuit circuit, StoredDocuments scope, Map<String, String> parameters) {
        List<Setting> with = parameters == null ? List.of() : parameters.entrySet().stream()
                .map(entry -> new Setting(entry.getKey(), new Formula(entry.getValue())))
                .toList();
        try {
            return new MeasuredCircuit(circuit, scope, BenchConditions.nominal(), List.of()).elaborated(with);
        } catch (IllegalArgumentException | IllegalStateException refused) {
            throw CadProblem.invalid(refused.getMessage());
        }
    }

    private String analysisOf(SimulationRequest request, Circuit circuit) {
        String analysis = request.analysis() != null ? request.analysis().strip()
                : circuit.run().map(Analysis::kind).orElse(SimulationVocabulary.TRANSIENT);
        if (!SimulationVocabulary.ANALYSES.contains(analysis)) {
            throw CadProblem.invalid("'" + analysis + "' is no analysis; a circuit runs as "
                                     + String.join(" or ", SimulationVocabulary.ANALYSES.stream().sorted().toList()) + ".");
        }
        return analysis;
    }

    /** The circuit with the analysis it runs as, for as long as the request asks — its own line's when it asks nothing. */
    private static Circuit withDuration(Circuit circuit, String analysis, Double duration) {
        double seconds = duration != null ? duration : circuit.run().map(Analysis::duration).orElse(0.0);
        if (analysis.equals(SimulationVocabulary.TRANSIENT) && seconds <= 0) {
            throw CadProblem.invalid("A transient runs for some time: send 'duration' in seconds, or write "
                                     + "'analysis transient for 20 ms' in the circuit.");
        }
        return new Circuit(circuit.head(), circuit.parameters(), circuit.parts(), circuit.layout(), circuit.probes(),
                           circuit.timeBase(), circuit.expectations(), new Analysis(analysis, seconds), circuit.views(),
                           circuit.frame(), circuit.boundary());
    }

    private int samplesOf(Integer asked) {
        int samples = asked == null ? 2000 : asked;
        if (samples < 2 || samples > properties.simulation().maximumSamples()) {
            throw CadProblem.invalid("A transient is read at 2–" + properties.simulation().maximumSamples() + " samples.");
        }
        return samples;
    }

    // ── The runs ─────────────────────────────────────────────────────────────────────────────────────────────────

    private static SimulationResult transientRun(String circuit, ElaboratedCircuit elaborated, int samples, boolean everyPart,
                                                 ComputationProgress progress) {
        TransientRun run;
        try {
            run = TransientRun.of(elaborated, samples, everyPart, progress);
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException failed) {
            throw new CadProblem.Unprocessable("simulation-failed", failed.getMessage());
        }
        return new Transient(SimulationVocabulary.TRANSIENT, circuit, run.times(), run.voltages(), run.lows(), run.highs(),
                             run.currents(), run.powers(), run.levels());
    }

    private static SimulationResult operatingPoint(String circuit, ElaboratedCircuit elaborated) {
        try {
            return new OperatingPoint(SimulationVocabulary.OPERATING, circuit, CircuitSimulation.operatingPoint(elaborated));
        } catch (IllegalArgumentException | IllegalStateException | ArithmeticException failed) {
            throw new CadProblem.Unprocessable("simulation-failed", failed.getMessage());
        }
    }

    // ── Who may run ──────────────────────────────────────────────────────────────────────────────────────────────

    /** The actor, allowed to run circuits — where the owner is, or anywhere for the catalogue's. */
    private String requireRun(DocumentOwner owner) {
        CadPlace place = owner == null ? CadPlace.installation() : places.requireOwner(owner);
        authority.require(CadCapability.SIMULATION_RUN, place);
        return authority.requireActor();
    }
}
