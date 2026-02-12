package org.jmouse.crawler.runtime.politeness;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.KeyAwarePolitenessPolicy;
import org.jmouse.crawler.api.ProcessingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public final class KeyedPolitenessPolicy<K extends PolitenessKey>
        implements KeyAwarePolitenessPolicy<K> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyedPolitenessPolicy.class);

    private final PolitenessKeyResolver<K>   resolver;
    private final ConcurrentMap<K, TimeGate> gates;
    private final Function<K, TimeGate>      gateFactory;

    public KeyedPolitenessPolicy(
            PolitenessKeyResolver<K> resolver,
            Function<K, TimeGate> gateFactory
    ) {
        this.resolver = Verify.nonNull(resolver, "resolver");
        this.gateFactory = Verify.nonNull(gateFactory, "gateFactory");
        this.gates = new ConcurrentHashMap<>();
    }

    @Override
    public Instant eligibleAt(ProcessingTask task, Instant now) {
        K        key      = keyOf(task);
        TimeGate gate     = gates.computeIfAbsent(key, gateFactory);
        Instant  eligible = gate.acquire(now);

        LOGGER.debug(
                "politeness.check key={} task={} now={} eligibleAt={}",
                key,
                task.url(),
                now,
                eligible
        );

        return eligible;
    }

    @Override
    public K keyOf(ProcessingTask task) {
        return resolver.resolve(task);
    }

}

