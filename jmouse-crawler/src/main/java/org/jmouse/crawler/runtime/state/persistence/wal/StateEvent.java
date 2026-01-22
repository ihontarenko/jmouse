package org.jmouse.crawler.runtime.state.persistence.wal;

import org.jmouse.crawler.api.DeadLetterEntry;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;

import java.net.URI;
import java.time.Instant;

public sealed interface StateEvent permits
        StateEvent.FrontierOffered,
        StateEvent.FrontierPolled,
        StateEvent.RetryScheduled,
        StateEvent.RetryDrained,
        StateEvent.InFlightAdded,
        StateEvent.InFlightRemoved,
        StateEvent.SeenDiscovered,
        StateEvent.SeenProcessed,
        StateEvent.DlqPut {

    String type();

    final class Types {
        public static final String FRONTIER_OFFERED = "frontier.offered";
        public static final String FRONTIER_POLLED  = "frontier.polled";
        public static final String RETRY_SCHEDULED  = "retry.scheduled";
        public static final String RETRY_DRAINED    = "retry.drained";
        public static final String INFLIGHT_ADDED   = "inflight.added";
        public static final String INFLIGHT_REMOVED = "inflight.removed";
        public static final String SEEN_DISCOVERED  = "seen.discovered";
        public static final String SEEN_PROCESSED   = "seen.processed";
        public static final String DLQ_PUT          = "dlq.put";
        private Types() {
        }
    }

    record FrontierOffered(ProcessingTask task) implements StateEvent {
        @Override
        public String type() {
            return Types.FRONTIER_OFFERED;
        }
    }

    record FrontierPolled(TaskId taskId) implements StateEvent {
        @Override
        public String type() {
            return Types.FRONTIER_POLLED;
        }
    }

    record RetryScheduled(ProcessingTask task, Instant eligibleAt, String reason) implements StateEvent {
        @Override
        public String type() {
            return Types.RETRY_SCHEDULED;
        }
    }

    record RetryDrained(TaskId taskId, Instant eligibleAt) implements StateEvent {
        @Override
        public String type() {
            return Types.RETRY_DRAINED;
        }
    }

    record InFlightAdded(ProcessingTask task) implements StateEvent {
        @Override
        public String type() {
            return Types.INFLIGHT_ADDED;
        }
    }

    record InFlightRemoved(TaskId taskId) implements StateEvent {
        @Override
        public String type() {
            return Types.INFLIGHT_REMOVED;
        }
    }

    record SeenDiscovered(URI url) implements StateEvent {
        @Override
        public String type() {
            return Types.SEEN_DISCOVERED;
        }
    }

    record SeenProcessed(URI url) implements StateEvent {
        @Override
        public String type() {
            return Types.SEEN_PROCESSED;
        }
    }

    record DlqPut(DeadLetterEntry entry) implements StateEvent {
        @Override
        public String type() {
            return Types.DLQ_PUT;
        }
    }
}
