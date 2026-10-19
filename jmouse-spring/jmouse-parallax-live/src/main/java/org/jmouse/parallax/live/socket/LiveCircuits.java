package org.jmouse.parallax.live.socket;

import jakarta.annotation.PreDestroy;
import org.jmouse.parallax.circuit.LiveControls;
import org.jmouse.parallax.script.engine.DefinedObject;
import org.jmouse.parallax.object.CompiledObject;
import org.jmouse.parallax.object.ParameterDefinition;
import org.jmouse.parallax.object.ObjectDefinition;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.live.LiveActivity;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.authority.ParallaxPlaces;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.ComponentModels;
import org.jmouse.parallax.jpa.catalogue.MathematicalObject;
import org.jmouse.parallax.store.catalogue.ObjectDocuments;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * What a running circuit needs from the stores: the object it runs, checked as the caller may read and run it, the
 * circuit compiled with the catalogue's components, and the controls its live parts offer. The stepping itself is the
 * library's ({@code LiveCircuitSession}); each socket drives one ({@link LiveRun}) on the shared clock here.
 */
public class LiveCircuits implements LiveActivity {

    /** How many circuits run at once on this server, all sockets together; the oldest is not stopped, a new one waits. */
    private static final int STEPPING_THREADS = 2;

    private final Catalogue                catalogue;
    private final ComponentModels          components;
    private final ObjectDocuments          documents;
    private final ParallaxAuthority        authority;
    private final ParallaxPlaces           places;
    private final ScheduledExecutorService clock = Executors.newScheduledThreadPool(STEPPING_THREADS, runnable -> {
        Thread thread = new Thread(runnable, "live-circuit");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Where captures are replayed, all sockets together, one at a time: apart from the stepping threads, so a stretch of
     * eighty thousand samples replayed finely never holds a running circuit's frames.
     */
    private final ExecutorService replays = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "live-capture");
        thread.setDaemon(true);
        return thread;
    });

    /** Circuits stepping now, on every socket: what the activity indicator shows the server is busy with. */
    private final java.util.concurrent.atomic.AtomicInteger running = new java.util.concurrent.atomic.AtomicInteger();

    public LiveCircuits(Catalogue catalogue, ComponentModels components, ObjectDocuments documents, ParallaxAuthority authority,
                        ParallaxPlaces places) {
        this.catalogue = catalogue;
        this.components = components;
        this.documents = documents;
        this.authority = authority;
        this.places = places;
    }

    /** A circuit ready to run: the object it belongs to, as compiled now, and what a reader can work while it runs. */
    public record Prepared(MathematicalObject object, ObjectDefinition.Circuit circuit, CompiledObject compiled,
                           List<LiveControls.LiveControl> controls) {
    }

    /**
     * The object a socket runs, refused unless the caller may read it and run computations where it lives — the same
     * rule as any computation.
     */
    @Transactional(readOnly = true)
    public MathematicalObject requireRunnable(String objectId) {
        if (objectId == null || objectId.isBlank()) {
            throw ParallaxProblem.invalid("Name the circuit to run.");
        }
        MathematicalObject object = catalogue.requireReadable(objectId);
        if (!object.isCatalogue()) {
            authority.require(ParallaxCapability.COMPUTE, places.requireOwner(object.owner().orElseThrow()));
        } else {
            authority.requireGlobally(ParallaxCapability.COMPUTE);
        }
        return object;
    }

    /**
     * The circuit compiled with these values, its parts resolved in the catalogue the object can see.
     *
     * @param parameters the sliders the circuit is compiled against: the edited text's own, or null for the saved
     *                   object's
     */
    public Prepared prepare(MathematicalObject object, ObjectDefinition.Circuit circuit,
                            List<ParameterDefinition> parameters, Map<String, Double> values) {
        CompiledObject compiled = parameters == null ? catalogue.compileVariant(object, circuit, values)
                : Catalogue.compileChecked(circuit, parameters, values, components.forCircuit(object));
        return new Prepared(object, circuit, compiled, LiveControls.of(circuit, components.forCircuit(object)));
    }

    /** The circuit as the object is saved, when the reader runs it unchanged. */
    @Transactional(readOnly = true)
    public ObjectDefinition.Circuit storedCircuit(MathematicalObject object) {
        if (!(catalogue.definitionOf(object) instanceof ObjectDefinition.Circuit circuit)) {
            throw ParallaxProblem.invalid("Only a circuit runs live.");
        }
        return circuit;
    }

    /** A circuit as written in a text — an edit of the document, run at once —, with the parameters it declares. */
    public DefinedObject circuitOf(String source) {
        DefinedObject defined = documents.read(source, "the edited circuit");
        if (!(defined.definition() instanceof ObjectDefinition.Circuit)) {
            throw ParallaxProblem.invalid("The text is not a circuit.");
        }
        return defined;
    }

    /** How many circuits are stepping now. */
    @Override
    public int running() {
        return running.get();
    }

    void started() {
        running.incrementAndGet();
    }

    void stopped() {
        running.decrementAndGet();
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
