package org.jmouse.crawler.dsl.builder;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.PolitenessPolicy;
import org.jmouse.crawler.runtime.politeness.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;

public final class LanePolitenessBuilder {

    private PolitenessKeyResolver<PolitenessKey> resolver;

    private final Map<String, Duration> lanes    = new LinkedHashMap<>();
    private       Duration              fallback = Duration.ZERO;

    public LanePolitenessBuilder keyResolver(PolitenessKeyResolver<PolitenessKey> resolver) {
        this.resolver = nonNull(resolver, "resolver");
        return this;
    }

    public LanePolitenessBuilder lane(String lane, Duration interval) {
        Verify.state(lane != null && !lane.isBlank(), "lane must be non-blank");
        lanes.put(lane, nonNull(interval, "interval"));
        return this;
    }

    public LanePolitenessBuilder fallback(Duration interval) {
        this.fallback = nonNull(interval, "fallback");
        return this;
    }

    public PolitenessPolicy build() {
        Verify.state(resolver != null, "politeness.keyResolver(...) is required");

        return new KeyedPolitenessPolicy<>(
                resolver,
                key -> new FixedIntervalGate(lanes.getOrDefault(key.lane(), fallback))
        );
    }
}
