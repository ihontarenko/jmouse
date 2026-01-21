package org.jmouse.crawler.runtime.politeness;

import org.jmouse.core.Verify;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.PolitenessPolicy;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public final class KeyedPolitenessPolicy<K> implements PolitenessPolicy {

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
        K        key  = resolver.resolve(task);
        TimeGate gate = gates.computeIfAbsent(key, gateFactory);
        return gate.eligibleAt(now);
    }
}

