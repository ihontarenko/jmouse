package org.jmouse.cad.live;

import java.util.Set;
import org.jmouse.cad.store.activity.LiveActivity;
import jakarta.annotation.PreDestroy;
import org.jmouse.cad.Formula;
import org.jmouse.cad.Setting;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.jpa.catalogue.DocumentStore;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.engine.LiveCircuitSession.LiveCircuit;
import org.jmouse.cad.simulation.engine.MeasuredCircuit;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * What a running circuit needs from the stores: the circuit it runs — a stored one the reader may see, or a text sent —,
 * checked as the reader may run circuits where it belongs, and the catalogue its parts are found in. The stepping itself is
 * the engine's ({@code LiveCircuitSession}); each socket drives one ({@link LiveRun}) on the shared clock here.
 */
public class LiveCircuits implements LiveActivity {

    /** How many circuits step at once on this server, all sockets together; a new one waits for a thread. */
    private static final int STEPPING_THREADS = 2;

    /** The runs open now, one a socket. */
    private final Set<LiveRun> open = ConcurrentHashMap.newKeySet();

    private final CadCatalogue        catalogue;
    private final DocumentAccess      access;
    private final DocumentStore       documents;
    private final DocumentReading     reading;
    private final CadAuthority        authority;
    private final CadPlaces           places;
    private final TransactionTemplate transactions;

    private final ScheduledExecutorService clock = Executors.newScheduledThreadPool(STEPPING_THREADS, runnable -> {
        Thread thread = new Thread(runnable, "live-circuit");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Where captures are replayed, all sockets together, one at a time: apart from the stepping threads, so a stretch
     * replayed finely never holds a running circuit's frames.
     */
    private final ExecutorService replays = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "live-capture");
        thread.setDaemon(true);
        return thread;
    });

    public LiveCircuits(CadCatalogue catalogue, DocumentAccess access, DocumentStore documents, DocumentReading reading,
                        CadAuthority authority, CadPlaces places, TransactionTemplate transactions) {
        this.catalogue = catalogue;
        this.access = access;
        this.documents = documents;
        this.reading = reading;
        this.authority = authority;
        this.places = places;
        this.transactions = transactions;
    }

    /**
     * Where a run reads its components: an owner's own and the catalogue's, as the reader may see them, each document read
     * once in a transaction of its own — a circuit elaborated again on the stepping thread, a part having failed, has no
     * request around it — and kept for the run.
     */
    /** A run its socket opened; counted until it is closed. */
    void opened(LiveRun run) {
        open.add(run);
    }

    /** A run its socket closed — or failed —: no longer counted, however many times it is said. */
    void closed(LiveRun run) {
        open.remove(run);
    }

    @Override
    public int running() {
        return open.size();
    }

    public record Scope(DocumentOwner owner, CatalogueDocuments documents) {
    }

    /** The place a run is for, refused unless the reader may run circuits there. */
    public Scope requireRunnable(String ownerAddress) {
        DocumentOwner owner = ownerAddress == null || ownerAddress.isBlank() ? null
                : DocumentOwner.parse(ownerAddress.strip())
                        .orElseThrow(() -> CadProblem.invalid("An owner is written TYPE:id — PROJECT:42."));
        CadPlace place = owner == null ? CadPlace.installation() : places.requireOwner(owner);
        authority.require(CadCapability.SIMULATION_RUN, place);
        StoredDocuments stored = catalogue.scope(owner);
        Map<String, Optional<DocumentNode>> read = new ConcurrentHashMap<>();
        CatalogueDocuments kept = slug -> read.computeIfAbsent(slug, wanted -> transactions.execute(status -> stored.find(wanted)));
        return new Scope(owner, kept);
    }

    /**
     * The circuit to run: a stored one the reader may see, by reference — or an unsaved text, sent as it is.
     *
     * @throws CadProblem when neither or both are given, the document is hidden, or it is no circuit
     */
    public Circuit circuitOf(Scope scope, String reference, String source) {
        if ((reference == null) == (source == null)) {
            throw CadProblem.invalid("Name a stored circuit as 'circuit' or send its text as 'source' — one of the two.");
        }
        ReadDocument read = source != null ? reading.read(source, "the circuit sent")
                : transactions.execute(status -> catalogue.scope(scope.owner()).resolve(reference)
                        .filter(found -> documents.findBySlug(found.index().slug())
                                .filter(document -> document.owner().isPresent() || access.canRead(document)).isPresent())
                        .orElseThrow(() -> CadProblem.notFound("The circuit")));
        if (!(read.model().orElse(null) instanceof Circuit circuit)) {
            throw CadProblem.invalid("'" + read.index().slug() + "' is a " + read.index().kind() + ", not a circuit: only a "
                                     + "circuit runs live.");
        }
        return circuit;
    }

    /**
     * The circuit as a session runs it: on a nominal bench, with what the reader set — a slider by its parameter's name,
     * a live part's setting as {@code S1.closed} —, each value as written.
     */
    public LiveCircuit live(Scope scope, Circuit circuit, Map<String, String> settings) {
        List<Setting> with = settings.entrySet().stream()
                .map(setting -> new Setting(setting.getKey(), new Formula(setting.getValue()))).toList();
        return new LiveCircuit(new MeasuredCircuit(circuit, scope.documents(), BenchConditions.nominal(), List.of()), with);
    }

    ScheduledExecutorService clock() {
        return clock;
    }

    ExecutorService replays() {
        return replays;
    }

    @PreDestroy
    void stop() {
        clock.shutdownNow();
        replays.shutdownNow();
    }
}
