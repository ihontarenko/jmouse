package org.jmouse.cad.store.simulation;

import org.jmouse.cad.store.authority.CadAuthority;
import org.jmouse.cad.store.authority.CadCapability;
import org.jmouse.cad.store.authority.CadPlace;
import org.jmouse.cad.store.simulation.SimulationViews.DocumentVerdicts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The published catalogue's expectations measured as one run in the background — every circuit against what it says of
 * itself, every component against its datasheet —: a run takes minutes, so it is started, followed by its progress and
 * read when it ends. One run at a time; the last one is kept until the next starts. Each document is read in a short
 * transaction of its own, never one held for the whole run.
 */
public class CatalogueChecks implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogueChecks.class);

    /** Where a run stands. */
    public enum State { IDLE, RUNNING, DONE, FAILED }

    /**
     * A run as it stands.
     *
     * @param done    the documents measured so far
     * @param total   the documents the run measures
     * @param results each document that states expectations — or could not be measured —, as measured so far
     * @param failure why the run stopped before its end; null otherwise
     */
    public record CheckRun(State state, int done, int total, Instant startedAt, Instant finishedAt,
                           List<DocumentVerdicts> results, String failure) {
    }

    private final Simulations            simulations;
    private final CadAuthority           authority;
    private final Clock                  clock;
    private final List<DocumentVerdicts> results = new ArrayList<>();
    private final ExecutorService        worker  = Executors.newSingleThreadExecutor(work -> {
        Thread thread = new Thread(work, "cad-catalogue-checks");
        thread.setDaemon(true);
        return thread;
    });

    private volatile State   state = State.IDLE;
    private volatile int     done;
    private volatile int     total;
    private volatile Instant startedAt;
    private volatile Instant finishedAt;
    private volatile String  failure;

    public CatalogueChecks(Simulations simulations, CadAuthority authority, Clock clock) {
        this.simulations = simulations;
        this.authority = authority;
        this.clock = clock;
    }

    /** Starts a run, or answers the one already running. */
    public synchronized CheckRun start() {
        requireRun();
        if (state == State.RUNNING) {
            return view();
        }
        List<String> slugs = simulations.catalogueCheckable();
        synchronized (results) {
            results.clear();
        }
        done = 0;
        total = slugs.size();
        startedAt = clock.instant();
        finishedAt = null;
        failure = null;
        state = State.RUNNING;
        worker.submit(() -> run(slugs));
        return view();
    }

    /** The run as it stands: the last one, or the one running. */
    public CheckRun current() {
        requireRun();
        return view();
    }

    @Override
    public void close() {
        worker.shutdownNow();
    }

    private void run(List<String> slugs) {
        try {
            for (String slug : slugs) {
                DocumentVerdicts measured = simulations.checkCatalogueDocument(slug);
                if (measured != null && (measured.refusal() != null || !measured.verdicts().isEmpty())) {
                    synchronized (results) {
                        results.add(measured);
                    }
                }
                done++;
            }
            state = State.DONE;
        } catch (RuntimeException stopped) {
            LOGGER.error("The catalogue check stopped", stopped);
            failure = stopped.getMessage();
            state = State.FAILED;
        } finally {
            finishedAt = clock.instant();
        }
    }

    private CheckRun view() {
        synchronized (results) {
            return new CheckRun(state, done, total, startedAt, finishedAt, List.copyOf(results), failure);
        }
    }

    private void requireRun() {
        authority.require(CadCapability.SIMULATION_RUN, CadPlace.installation());
    }
}
