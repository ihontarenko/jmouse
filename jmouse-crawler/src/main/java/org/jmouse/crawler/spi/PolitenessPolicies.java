package org.jmouse.crawler.spi;

import java.time.Duration;
import java.util.List;

/**
 * Factory and composition utilities for {@link PolitenessPolicy} instances. 🧰
 *
 * <p>This class provides convenient, intention-revealing factory methods for
 * common politeness configurations used by crawlers.</p>
 *
 * <p>All returned policies are immutable with respect to their configuration
 * (though some may maintain internal state for rate-limiting).</p>
 */
public final class PolitenessPolicies {

    private PolitenessPolicies() {
        // utility class
    }

    /**
     * Return a politeness policy that applies no restrictions.
     *
     * <p>All requests are immediately eligible.</p>
     *
     * @return no-op politeness policy
     */
    public static PolitenessPolicy none() {
        return NoopPolitenessPolicy.INSTANCE;
    }

    /**
     * Compose multiple politeness policies using <em>most-restrictive</em> semantics.
     *
     * <p>The resulting policy returns the maximum (latest) eligibility instant
     * among all provided policies.</p>
     *
     * @param politenessPolicies policies to combine
     * @return composite politeness policy
     */
    public static PolitenessPolicy allOf(PolitenessPolicy... politenessPolicies) {
        return new CompositePolitenessPolicy(List.of(politenessPolicies));
    }

    /**
     * Create a balanced, "gentle" politeness policy suitable for general-purpose crawling.
     *
     * <p>This policy combines:</p>
     * <ul>
     *   <li>a per-host minimum delay</li>
     *   <li>a global requests-per-second (RPS) limit</li>
     * </ul>
     *
     * <p>The most restrictive constraint always wins.</p>
     *
     * @param perHostMinDelayMillis minimum delay between requests to the same host (milliseconds)
     * @param globalMaxRps          maximum global requests per second
     * @return composite politeness policy
     */
    public static PolitenessPolicy gentle(long perHostMinDelayMillis, double globalMaxRps) {
        return allOf(
                new PerHostMinDelayPolitenessPolicy(Duration.ofMillis(perHostMinDelayMillis)),
                new RPSPolitenessPolicy(globalMaxRps)
        );
    }

    /**
     * Create a politeness policy that enforces a minimum delay per host.
     *
     * @param perHostMinDelayMillis minimum delay between requests to the same host (milliseconds)
     * @return per-host delay politeness policy
     */
    public static PolitenessPolicy perHostDelay(long perHostMinDelayMillis) {
        return new PerHostMinDelayPolitenessPolicy(Duration.ofMillis(perHostMinDelayMillis));
    }

    /**
     * Create a politeness policy that enforces a global requests-per-second limit.
     *
     * @param globalMaxRps maximum allowed requests per second
     * @return global RPS politeness policy
     */
    public static PolitenessPolicy globalRps(double globalMaxRps) {
        return new RPSPolitenessPolicy(globalMaxRps);
    }
}
