package org.jmouse.crawler.smoke;

import org.jmouse.crawler.core.CrawlTask;
import org.jmouse.crawler.routing.*;
import org.jmouse.crawler.routing.CrawlRouteResolver;
import org.jmouse.crawler.runtime.*;
import org.jmouse.crawler.runtime.impl.InMemoryRetryBuffer;
import org.jmouse.crawler.spi.*;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public final class Smoke1 {

    public static void main(String[] args) throws Exception {

        System.out.println("== ExecutorRunner smoke test started ==");

        // --- deterministic clock
        Clock clock = Clock.fixed(
                Instant.parse("2026-01-14T00:00:00Z"),
                ZoneOffset.UTC
        );

        // --- thread-safe SeenStore
        SeenStore seen = new SeenStore() {
            private final Set<URI> set = ConcurrentHashMap.newKeySet();
            @Override
            public boolean firstTime(URI url) {
                return set.add(url);
            }
        };

        // --- allow all
        ScopePolicy scope = new ScopePolicy() {
            @Override public boolean isAllowed(CrawlTask task) { return true; }
            @Override public String denyReason(CrawlTask task) { return "denied"; }
        };

        // --- retry policy:
        // RetryMe  -> retry once immediately
        // DeadMe   -> DLQ
        RetryPolicy retryPolicy = new RetryPolicy() {
            @Override
            public RetryDecision onFailure(CrawlTask task, Throwable error, Instant now) {
                if (error instanceof RetryMe) {
                    return RetryDecision.retry(now, "retry-me");
                }
                if (error instanceof DeadMe) {
                    return RetryDecision.deadLetter("dead-me");
                }
                return RetryDecision.deadLetter("unknown");
            }
        };

        // --- minimal fetcher/parsers (not actually used)
        Fetcher fetcher = req ->
                new FetchResult(req.url(), 200, Map.of(), new byte[0], "text/plain");
        ParserRegistry parsers = ct -> null;

        Frontier frontier = new FifoFrontier();
        RetryBuffer retryBuffer = new InMemoryRetryBuffer();
        DeadLetterQueue dlq = new InMemoryDeadLetterQueue();

        // --- pipeline logic
        // n % 10 == 0  -> DLQ
        // n % 3 == 0   -> retry once, then success
        // else         -> success
        CrawlPipeline pipeline = new StepsPipeline("smoke", List.of(
                new StepsPipeline.NamedStep("work", ctx -> {
                    int n = extractIndex(ctx.task().url());

                    if (n % 10 == 0) {
                        throw new DeadMe();
                    }
                    if (n % 3 == 0 && ctx.task().attempt() == 0) {
                        throw new RetryMe();
                    }
                    return PipelineResult.ok("work");
                })
        ));

        CrawlRoute route = new SimpleCrawlRoute(
                "all",
                UrlMatches.any(),
                pipeline,
                new HashSet<>()
        );

        CrawlRouteResolver routes =
                new FirstMatchCrawlRouteResolver(List.of(route));

        CrawlRunContext run = new DefaultCrawlRunContext(
                frontier,
                retryBuffer,
                dlq,
                routes,
                fetcher,
                parsers,
                seen,
                scope,
                retryPolicy,
                clock,
                null
        );

        SimpleCrawlEngine engine = new SimpleCrawlEngine(run);

        // --- executor runner
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CrawlRunner runner = new ExecutorRunner(pool, 64);

        // --- seed 1000 tasks
        for (int i = 1; i <= 1000; i++) {
            engine.submit(new CrawlTask(
                    URI.create("https://example.test/task/" + i),
                    0,
                    null,
                    "seed",
                    0,
                    clock.instant(),
                    0,
                    null
            ));
        }

        // --- run with timeout guard
        Future<?> future = pool.submit(() -> runner.runUntilDrained(engine));

        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.err.println("❌ FAILED: execution timed out (hang detected)");
            pool.shutdownNow();
            System.exit(1);
        }

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        // --- assertions (manual)
        int frontierSize = run.frontier().size();
        int retrySize = run.retryBuffer().size();
        int dlqSize = run.deadLetterQueue().size();

        System.out.println("Frontier size   = " + frontierSize);
        System.out.println("RetryBuffer size= " + retrySize);
        System.out.println("DLQ size        = " + dlqSize);

        if (frontierSize != 0) {
            fail("Frontier not empty");
        }
        if (retrySize != 0) {
            fail("RetryBuffer not empty");
        }
        if (dlqSize != 100) {
            fail("DLQ size expected 100, got " + dlqSize);
        }

        System.out.println("✅ PASSED: ExecutorRunner smoke test");
    }

    // --- helpers ------------------------------------------------------------

    private static int extractIndex(URI uri) {
        String path = uri.getPath();
        return Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
    }

    private static void fail(String message) {
        System.err.println("❌ FAILED: " + message);
        System.exit(1);
    }

    // --- test exceptions ----------------------------------------------------

    static final class RetryMe extends RuntimeException {}
    static final class DeadMe extends RuntimeException {}
}
