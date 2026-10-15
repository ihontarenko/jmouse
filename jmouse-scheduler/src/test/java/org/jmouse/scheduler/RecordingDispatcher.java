package org.jmouse.scheduler;

import org.jmouse.scheduler.spi.TriggerDispatcher;
import org.jmouse.scheduler.spi.TriggerHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Remembers every occurrence it was handed, in order. Thread-safe. */
final class RecordingDispatcher implements TriggerDispatcher {

    private final List<Occurrence> occurrences = new CopyOnWriteArrayList<>();

    @Override
    public void dispatch(Occurrence occurrence) {
        occurrences.add(occurrence);
    }

    List<Occurrence> occurrences() {
        return List.copyOf(occurrences);
    }

    List<Instant> scheduledTimes() {
        List<Instant> times = new ArrayList<>();

        for (Occurrence occurrence : occurrences) {
            times.add(occurrence.scheduledAt());
        }

        return times;
    }

    /** A handler that does nothing, registered under {@code key}. */
    static TriggerHandler handler(String key) {
        return new TriggerHandler() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public void handle(Occurrence occurrence) {
            }
        };
    }
}
