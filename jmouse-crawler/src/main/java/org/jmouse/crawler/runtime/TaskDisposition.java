package org.jmouse.crawler.runtime;

import org.jmouse.crawler.routing.PipelineResult;

import java.time.Instant;

public sealed interface TaskDisposition
        permits TaskDisposition.Completed,
                TaskDisposition.RetryLater,
                TaskDisposition.DeadLetter,
                TaskDisposition.Discarded {

    record Completed(
            PipelineResult result
    ) implements TaskDisposition {}

    record RetryLater(
            Instant notBefore,
            String reason,
            Throwable error,
            String stageId,
            String routeId
    ) implements TaskDisposition {}

    record DeadLetter(
            String reason,
            Throwable error,
            String stageId,
            String routeId
    ) implements TaskDisposition {}

    record Discarded(String reason) implements TaskDisposition {}

    static Completed completed(PipelineResult result) {
        return new Completed(result);
    }

    static RetryLater retryLater(Instant notBefore, String reason, Throwable error, String stageId, String routeId) {
        return new RetryLater(notBefore, reason, error, stageId, routeId);
    }

    static DeadLetter deadLetter(String reason, Throwable error, String stageId, String routeId) {
        return new DeadLetter(reason, error, stageId, routeId);
    }

    static Discarded discarded(String reason) { return new Discarded(reason); }
}
