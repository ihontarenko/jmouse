package org.jmouse.crawler.spi;

import java.time.Duration;
import java.util.List;

public final class PolitenessPolicies {

    private PolitenessPolicies() {}

    public static PolitenessPolicy none() {
        return NoopPolitenessPolicy.INSTANCE;
    }

    public static PolitenessPolicy gentle(long perHostMinDelayMillis, double globalMaxRps) {
        return new CompositePolitenessPolicy(List.of(
                new PerHostMinDelayPolitenessPolicy(Duration.ofMillis(perHostMinDelayMillis)),
                new RPSPolitenessPolicy(globalMaxRps)
        ));
    }

    public static PolitenessPolicy perHostDelay(long perHostMinDelayMillis) {
        return new PerHostMinDelayPolitenessPolicy(Duration.ofMillis(perHostMinDelayMillis));
    }

    public static PolitenessPolicy globalRps(double globalMaxRps) {
        return new RPSPolitenessPolicy(globalMaxRps);
    }
}