package org.jmouse.crawler.runtime.state.persistence;

import java.time.Duration;

public sealed interface Durability permits Durability.Sync, Durability.Batched, Durability.Async {

    static Durability sync() {
        return new Sync();
    }

    static Durability batched(int maxRecords, Duration maxDelay) {
        return new Batched(maxRecords, maxDelay);
    }

    static Durability async(Duration flushInterval) {
        return new Async(flushInterval);
    }

    /** fsync on every append */
    record Sync() implements Durability {}

    /** fsync after N records or T time */
    record Batched(int maxRecords, Duration maxDelay) implements Durability {}

    /** fsync periodically */
    record Async(Duration flushInterval) implements Durability {}
}
