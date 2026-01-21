package org.jmouse.crawler.runtime.politeness;

import java.time.Instant;

public interface TimeGate {
    Instant acquire(Instant now);
}