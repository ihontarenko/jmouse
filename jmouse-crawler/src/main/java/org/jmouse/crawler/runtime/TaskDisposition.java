package org.jmouse.crawler.runtime;

import java.time.Instant;

public sealed interface TaskDisposition
        permits TaskDisposition.Completed,
        TaskDisposition.RetryLater,
        TaskDisposition.DeadLetter,
        TaskDisposition.Discarded {

    record Completed() implements TaskDisposition {}

    record RetryLater(Instant notBefore, String reason, Throwable error) implements TaskDisposition {}

    record DeadLetter(String reason, Throwable error) implements TaskDisposition {}

    record Discarded(String reason) implements TaskDisposition {}

    static Completed completed() {
        return new Completed();
    }

    static RetryLater retryLater(Instant notBefore, String reason, Throwable error) {
        return new RetryLater(notBefore, reason, error);
    }

    static DeadLetter deadLetter(String reason, Throwable error) {
        return new DeadLetter(reason, error);
    }

    static Discarded discarded(String reason) {
        return new Discarded(reason);
    }
}
