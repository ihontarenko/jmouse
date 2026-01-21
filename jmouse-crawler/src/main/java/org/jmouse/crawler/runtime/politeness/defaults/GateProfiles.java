package org.jmouse.crawler.runtime.politeness.defaults;

import org.jmouse.crawler.runtime.politeness.FixedIntervalGate;
import org.jmouse.crawler.runtime.politeness.PolitenessKey;
import org.jmouse.crawler.runtime.politeness.TimeGate;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

public final class GateProfiles {

    private GateProfiles() {}

    public static Function<PolitenessKey, TimeGate> byLane(Map<String, Duration> intervals, Duration fallback) {
        return key -> new FixedIntervalGate(intervals.getOrDefault(key.lane(), fallback));
    }

}