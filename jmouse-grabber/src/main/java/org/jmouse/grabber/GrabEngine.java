package org.jmouse.grabber;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.random.RandomGenerator;

import org.jmouse.core.chain.Chain;
import org.jmouse.grabber.document.PageDocument;
import org.jmouse.grabber.engine.PacingGate;
import org.jmouse.grabber.fetch.PageFetcher;
import org.jmouse.grabber.fetch.PageRequest;
import org.jmouse.grabber.fetch.PageResponse;
import org.jmouse.grabber.journal.VisitJournal;
import org.jmouse.grabber.journal.VisitQueue;
import org.jmouse.grabber.route.GrabRoute;
import org.jmouse.grabber.route.RouteTable;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpHeader;

/**
 * ⚙️ What actually happens to a visit.
 *
 * <p>A dispatcher takes visits off the queue and hands each to a virtual thread, bounded by the
 * policy's concurrency. The run ends when the queue drains with nothing in flight, when a cap is
 * reached, or when something says stop.</p>
 *
 * <p>⚠️ <b>The pacing gate is the only coordination point</b>, which is why a virtual thread per
 * in-flight visit is the right model: a worker waiting on a host's delay costs a few hundred bytes
 * rather than a thread.</p>
 */
final class GrabEngine {

    private final RouteTable                              routes;
    private final GrabPolicy                              policy;
    private final PageFetcher                             fetcher;
    private final Chain<Void, PageResponse, PageDocument> parsers;
    private final VisitQueue                              queue;
    private final VisitJournal                            journal;
    private final AddressNormalizer                       normalizer;
    private final ItemSink                                items;
    private final FailureSink                             failures;
    private final GrabListener                            listener;
    private final PacingGate                              gate;
    private final GrabRun                                 run = new GrabRun();

    GrabEngine(
            RouteTable routes,
            GrabPolicy policy,
            PageFetcher fetcher,
            Chain<Void, PageResponse, PageDocument> parsers,
            VisitQueue queue,
            VisitJournal journal,
            AddressNormalizer normalizer,
            ItemSink items,
            FailureSink failures,
            GrabListener listener,
            RandomGenerator random
    ) {
        this.routes = routes;
        this.policy = policy;
        this.fetcher = fetcher;
        this.parsers = parsers;
        this.queue = queue;
        this.journal = journal;
        this.normalizer = normalizer;
        this.items = items;
        this.failures = failures;
        this.listener = listener;
        this.gate = new PacingGate(policy, random);
    }

    GrabRun run() {
        return run;
    }

    VisitJournal journal() {
        return journal;
    }

    /**
     * 🌱 Puts the seeds and anything the last run left outstanding into the queue.
     *
     * <p>⚠️ <b>The outstanding visits skip the seen check on purpose.</b> They were marked seen when
     * they were first queued and were never finished, so asking again would refuse every one of them
     * and resume would silently do nothing — which is the failure mode a resumable crawler must not
     * have.</p>
     */
    void prime(List<Visit> seeds) {
        for (Visit pending : journal.pending()) {
            queue.offer(pending);
            run.countQueued();
        }

        seeds.forEach(this::enqueue);
    }

    /**
     * ➕ Queues a visit, unless something says not to.
     *
     * @return whether it was queued — a caller reporting what it followed needs the answer, and a
     *         caller that does not can ignore it
     */
    boolean enqueue(Visit visit) {
        if (run.isStopped()) {
            return false;
        }

        if (!policy.allowsDepth(visit.depth())) {
            return false;
        }

        if (!policy.allowsAnotherPage(queue.dispensed() + queue.size())) {
            return false;
        }

        // ⚠️ The address is normalised HERE, so what is fetched and what is remembered are the same
        // string. Normalising only the key would leave a run that correctly refuses to fetch a page
        // twice while still sending somebody's tracking parameter to the server on the one fetch it
        // does make, and resolving that page's links against an address the site did not mean.
        Visit    normalized = visit.at(normalizer.normalize(visit.address()));
        VisitKey key        = keyOf(normalized);

        if (!journal.markSeen(key)) {
            return false;
        }

        queue.offer(normalized);
        journal.remember(key, normalized);
        run.countQueued();
        listener.queued(normalized);

        return true;
    }

    void emit(Visit visit, Object item) {
        items.accept(item);
        run.countEmitted();
        listener.emitted(visit, item);
    }

    /**
     * ▶️ Runs until the queue drains, a cap is reached, or something stops it.
     */
    void drain() {
        Semaphore    permits  = new Semaphore(policy.concurrency());
        AtomicInteger inFlight = new AtomicInteger();

        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            while (!run.isStopped()) {
                Optional<Visit> next = queue.poll();

                if (next.isEmpty()) {
                    if (inFlight.get() == 0) {
                        break;
                    }

                    // Something is still working and may yet queue more. A millisecond is short
                    // enough not to delay the run and long enough not to be a busy loop.
                    LockSupport.parkNanos(1_000_000L);
                    continue;
                }

                Visit visit = next.get();

                permits.acquire();
                inFlight.incrementAndGet();

                workers.submit(() -> {
                    try {
                        process(visit);
                    } finally {
                        inFlight.decrementAndGet();
                        permits.release();
                    }
                });
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            run.stop("interrupted");
        }

        run.markFinished();
        listener.finished(run);
    }

    /**
     * 📄 One visit, end to end. Never throws: a handler that blows up fails its own visit and the run
     * carries on.
     */
    private void process(Visit visit) {
        VisitKey key = keyOf(visit);

        try {
            Optional<GrabRoute> claimed = routes.resolve(visit);

            if (claimed.isEmpty()) {
                run.countSkipped();
                listener.decided(visit, null, Verdict.skipped("no route claims this address"));
                journal.forget(key);
                return;
            }

            GrabRoute  route       = claimed.get();
            GrabPolicy routePolicy = route.policyWithin(policy);
            Verdict    verdict     = attempt(visit, route, routePolicy);

            apply(visit, route, verdict, routePolicy);

            // ⚠️ A page is taken off the frontier only when it was DEALT WITH. A run that stops
            // half way through one has not dealt with it, and forgetting it here would mean the
            // resume that this journal exists for silently skips the page the run died on — the
            // single worst thing a resumable crawler can do, because it looks like it worked.
            if (!verdict.endsRun()) {
                journal.forget(key);
            }
        } catch (RuntimeException exception) {
            record(visit, null, exception, true);
            journal.forget(key);
        }
    }

    /**
     * 🔁 Fetches, parses and hands the page to its handler, retrying what is worth retrying.
     *
     * <p>Retries happen on this worker rather than by re-queueing. On a virtual thread the wait is
     * free, and keeping a visit on one worker means the backoff is honoured exactly rather than
     * becoming a race with whatever else is in the queue.</p>
     */
    private Verdict attempt(Visit visit, GrabRoute route, GrabPolicy routePolicy) {
        Visit attempting = visit;

        while (true) {
            Verdict verdict;

            try {
                verdict = fetchAndHandle(attempting, route, routePolicy);
            } catch (RuntimeException exception) {
                boolean exhausted = attempting.attempt() >= routePolicy.retries();

                record(attempting, route.name(), exception, exhausted);

                if (exhausted) {
                    return Verdict.skipped("failed: " + exception.getMessage());
                }

                waitBackoff(routePolicy, attempting.attempt());
                attempting = attempting.retried();
                run.countRetried();
                continue;
            }

            if (verdict instanceof Verdict.Retry retry) {
                if (attempting.attempt() >= routePolicy.retries()) {
                    return Verdict.skipped("retries exhausted: " + retry.reason());
                }

                sleep(retry.after().toMillis());
                attempting = attempting.retried();
                run.countRetried();
                continue;
            }

            return verdict;
        }
    }

    private Verdict fetchAndHandle(Visit visit, GrabRoute route, GrabPolicy routePolicy) {
        gate.pass(visit.address());
        listener.requesting(visit);

        PageResponse response = fetcher.fetch(request(visit, routePolicy));

        run.countFetched();
        listener.fetched(visit, response);

        PageDocument document = parsers.perform(null, response);

        if (document == null) {
            return Verdict.skipped("no parser reads " + response.contentType());
        }

        DefaultPageContext page = new DefaultPageContext(visit, route.name(), response, document, this);

        route.identity().ifPresent(identity -> journal.markSeen(identity.apply(page)));

        return route.handler().handle(page);
    }

    private void apply(Visit visit, GrabRoute route, Verdict verdict, GrabPolicy routePolicy) {
        listener.decided(visit, route.name(), verdict);

        switch (verdict) {
            case Verdict.Skipped ignored -> run.countSkipped();
            case Verdict.Stopped stopped -> run.stop(stopped.reason());
            case Verdict.Followed ignored -> {
            }
            case Verdict.Leaf ignored -> {
            }
            case Verdict.Retry ignored -> {
                // attempt() resolves every retry before returning, so one arriving here means the
                // budget ran out and it was already turned into a skip.
                run.countSkipped();
            }
        }
    }

    private PageRequest request(Visit visit, GrabPolicy routePolicy) {
        Headers headers = new Headers();

        headers.setUserAgent(routePolicy.userAgent());
        routePolicy.headers().forEach((name, value) -> {
            HttpHeader header = HttpHeader.ofHeader(name);

            if (header != null) {
                headers.setHeader(header, value);
            }
        });

        if (!visit.origin().isSeed() && visit.origin().address() != null) {
            headers.setReferer(visit.origin().address().toString());
        }

        return PageRequest.get(visit.address(), headers, routePolicy.timeout());
    }

    private void record(Visit visit, String route, Throwable cause, boolean exhausted) {
        Failure failure = Failure.of(visit, route, cause, exhausted);

        if (exhausted) {
            run.countFailed();
        }

        failures.accept(failure);
        listener.failed(failure);
    }

    private void waitBackoff(GrabPolicy routePolicy, int attempt) {
        sleep(routePolicy.retryBackoff().sleep(attempt));
    }

    private void sleep(long milliseconds) {
        if (milliseconds <= 0) {
            return;
        }

        LockSupport.parkNanos(milliseconds * 1_000_000L);
    }

    private VisitKey keyOf(Visit visit) {
        return VisitKey.ofAddress(visit.address(), normalizer);
    }

}
