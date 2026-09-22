package org.jmouse.grabber.smoke;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.jmouse.core.matcher.TextMatchers;
import org.jmouse.grabber.GrabListener;
import org.jmouse.grabber.Grabber;
import org.jmouse.grabber.Visit;

/**
 * 🧪 That the pacing gate actually paces.
 *
 * <p>A delay nobody measured is a delay nobody has, and this is the setting whose whole purpose is to
 * keep a run from getting somebody's address blocked. So the intervals between requests are recorded
 * and reported rather than assumed.</p>
 *
 * <p>⚠️ It measures a floor, not an average. The gate guarantees that two requests to one host are at
 * least the configured distance apart; it promises nothing about how much longer than that they might
 * be, and on a loaded machine every interval will be a little over.</p>
 */
public final class PacingSmoke {

    private static final Duration DELAY = Duration.ofMillis(200);

    public static void main(String[] arguments) {
        List<Long> stamps = new ArrayList<>();

        // ⚠️ requesting(), not fetched(). The gate spaces out when requests GO OUT; the interval
        // between two replies also contains however long each request took, so a slow first page and
        // a fast second one make a correctly paced run look like it broke its own delay. That is what
        // this smoke reported the first time it was run.
        GrabListener stopwatch = new GrabListener() {
            @Override
            public synchronized void requesting(Visit visit) {
                stamps.add(System.nanoTime());
            }
        };

        Grabber grabber = Grabber.builder()
                .fetcher(Fixtures.fetcher())
                // One worker, because the guarantee is per host and two workers on one host would be
                // measuring the queue rather than the gate.
                .policy(policy -> policy.concurrency(1).delay(DELAY))
                .seed(Fixtures.CATALOG)
                .route("listing", TextMatchers.contains("/catalog"), page -> page.follow("a.product-link"))
                .route("product", TextMatchers.contains("/product/"), page -> page.leaf())
                .listener(stopwatch)
                .build();

        grabber.run();

        System.out.println("== intervals, asked for " + DELAY.toMillis() + "ms ==");

        boolean every = true;

        for (int index = 1; index < stamps.size(); index++) {
            long gap = (stamps.get(index) - stamps.get(index - 1)) / 1_000_000L;
            boolean held = gap >= DELAY.toMillis();

            every &= held;

            System.out.println("  " + gap + "ms " + (held ? "" : "  <- shorter than asked for"));
        }

        System.out.println("== checks ==");
        System.out.println((stamps.size() >= 3 ? "  OK   " : "  FAIL ") + "enough pages to measure ("
                                   + stamps.size() + ")");
        System.out.println((every ? "  OK   " : "  FAIL ") + "every interval was at least the configured delay");
    }

}
