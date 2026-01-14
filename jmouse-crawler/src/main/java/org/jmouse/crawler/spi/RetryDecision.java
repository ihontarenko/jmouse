package org.jmouse.crawler.spi;

import java.time.Instant;

public sealed interface RetryDecision permits RetryDecision.Retry, RetryDecision.DeadLetter, RetryDecision.Discard {

    static Retry retry(Instant notBefore, String reason) {
        return new Retry(notBefore, reason);
    }

    static DeadLetter deadLetter(String reason) {
        return new DeadLetter(reason);
    }

    static Discard discard(String reason) {
        return new Discard(reason);
    }

    record Retry(Instant notBefore, String reason) implements RetryDecision {
    }

    record DeadLetter(String reason) implements RetryDecision {
    }

    record Discard(String reason) implements RetryDecision {
    }

}
