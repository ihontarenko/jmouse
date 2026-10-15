package org.jmouse.parallax.store.computation;

import org.jmouse.parallax.Computations;
import org.jmouse.parallax.RepresentationKind;
import org.jmouse.parallax.authority.ParallaxAuthority;
import org.jmouse.parallax.authority.ParallaxCapability;
import org.jmouse.parallax.store.catalogue.Catalogue;
import org.jmouse.parallax.store.catalogue.Catalogue.Evaluable;
import org.jmouse.parallax.event.CatalogueWarmed;
import org.jmouse.parallax.limits.ParallaxLimits;
import org.jmouse.parallax.problem.ParallaxProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Precomputes the published catalogue at its defaults — every object, every representation that applies — so a
 * reader opening the Library or an object's scene is answered from the cache instead of waiting. Light requests
 * are computed here, one after another on one background thread; heavy ones are handed to the job queue.
 * Runs after start-up when {@link Parameter#CATALOGUE_WARMUP} is on, and on an administrator's request.
 */
public class CatalogueWarmup {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogueWarmup.class);

    public record WarmupStatus(boolean running, Instant startedAt, Instant finishedAt, int requests, int computed,
                               int queued, int alreadyCached, int failed) {

        static WarmupStatus idle() {
            return new WarmupStatus(false, null, null, 0, 0, 0, 0, 0);
        }
    }

    private final Catalogue catalogue;
    private final Computations computations;
    private final Computing computing;
    private final ParallaxAuthority authority;
    private final ParallaxLimits limits;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final AtomicReference<WarmupStatus> status = new AtomicReference<>(WarmupStatus.idle());

    public CatalogueWarmup(Catalogue catalogue, Computations computations, Computing computing, ParallaxAuthority authority,
                           ParallaxLimits limits, ApplicationEventPublisher events, Clock clock) {
        this.catalogue = catalogue;
        this.computations = computations;
        this.computing = computing;
        this.authority = authority;
        this.limits = limits;
        this.events = events;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmAfterStart() {
        if (limits.catalogueWarmupEnabled()) {
            begin();
        }
    }

    /** Starts a pass unless one is running; answers the status either way. */
    public WarmupStatus start() {
        authority.requireGlobally(ParallaxCapability.MANAGEMENT_WRITE);
        return begin();
    }

    public WarmupStatus status() {
        authority.requireGlobally(ParallaxCapability.MANAGEMENT_READ);
        return status.get();
    }

    private WarmupStatus begin() {
        WarmupStatus current = status.get();
        if (current.running()) {
            return current;
        }
        WarmupStatus started = new WarmupStatus(true, clock.instant(), null, 0, 0, 0, 0, 0);
        if (!status.compareAndSet(current, started)) {
            return status.get();
        }
        Thread.ofVirtual().name("catalogue-warmup").start(this::warm);
        return started;
    }

    private void warm() {
        int requests = 0;
        int computed = 0;
        int queued = 0;
        int cached = 0;
        int failed = 0;
        try {
            List<Evaluable> objects = catalogue.evaluables(null);
            for (Evaluable object : objects) {
                for (RepresentationKind representation : computations.applicableTo(object.definition(), object.parameters())) {
                    requests++;
                    ComputationRequest request = new ComputationRequest(null, object.objectId(), null, null, Map.of(), Map.of(), representation,
                            Map.of(), null, null, ComputationRequest.Mode.AUTOMATIC);
                    try {
                        var uncached = computing.uncached(request);
                        if (uncached.isEmpty()) {
                            cached++;
                        } else if (uncached.get().cost() <= computing.inlineBudget()
                                && computing.runWithinDeadline(uncached.get()).isPresent()) {
                            computed++;
                        } else {
                            // Too heavy by its estimate, or it overran the inline deadline: the worker takes it.
                            computing.enqueue(uncached.get(), request, true);
                            queued++;
                        }
                    } catch (ParallaxProblem | IllegalArgumentException refused) {
                        failed++;
                        LOGGER.debug("Warm-up skipped {} / {}: {}", object.objectId(), representation, refused.getMessage());
                    }
                    status.set(new WarmupStatus(true, status.get().startedAt(), null, requests, computed, queued, cached, failed));
                }
            }
        } catch (RuntimeException failure) {
            LOGGER.warn("The catalogue warm-up stopped", failure);
        } finally {
            status.set(new WarmupStatus(false, status.get().startedAt(), clock.instant(), requests, computed, queued, cached, failed));
            events.publishEvent(new CatalogueWarmed(computed, queued, cached, failed));
        }
    }
}
