package org.jmouse.crawler.runtime.state.persistence.wal;

import org.jmouse.crawler.api.DeadLetterEntry;
import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.TaskId;

import java.net.URI;
import java.time.Instant;

public sealed interface StateEvent
        permits StateEvent.FrontierOffered, StateEvent.FrontierPolled, StateEvent.RetryScheduled, StateEvent.RetryDrained, StateEvent.InFlightAdded, StateEvent.InFlightRemoved, StateEvent.SeenDiscovered, StateEvent.SeenProcessed, StateEvent.DlqPut {

    String type();

    record FrontierOffered(ProcessingTask task) implements StateEvent {
        @Override
        public String type() {
            return "frontier.offered";
        }
    }

    record FrontierPolled(TaskId taskId) implements StateEvent {
        @Override
        public String type() {
            return "frontier.polled";
        }
    }

    record RetryScheduled(ProcessingTask task, Instant eligibleAt, String reason) implements StateEvent {
        @Override
        public String type() {
            return "retry.scheduled";
        }
    }

    record RetryDrained(TaskId taskId, Instant eligibleAt) implements StateEvent {
        @Override
        public String type() {
            return "retry.drained";
        }
    }

    record InFlightAdded(ProcessingTask task) implements StateEvent {
        @Override
        public String type() {
            return "inflight.added";
        }
    }

    record InFlightRemoved(TaskId taskId) implements StateEvent {
        @Override
        public String type() {
            return "inflight.removed";
        }
    }

    record SeenDiscovered(URI url) implements StateEvent {
        @Override
        public String type() {
            return "seen.discovered";
        }
    }

    record SeenProcessed(URI url) implements StateEvent {
        @Override
        public String type() {
            return "seen.processed";
        }
    }

    record DlqPut(DeadLetterEntry entry) implements StateEvent {
        @Override
        public String type() {
            return "dlq.put";
        }
    }
}
