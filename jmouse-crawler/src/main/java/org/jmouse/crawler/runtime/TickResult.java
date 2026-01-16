package org.jmouse.crawler.runtime;

import java.time.Instant;

public sealed interface TickResult permits TickResult.DidWork, TickResult.Idle, TickResult.Drained {
    record DidWork() implements TickResult {}
    record Idle(Instant notBefore) implements TickResult {} // коли має сенс прокинутися
    record Drained() implements TickResult {}
}