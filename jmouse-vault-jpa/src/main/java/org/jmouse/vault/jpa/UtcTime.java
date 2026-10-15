package org.jmouse.vault.jpa;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** 🕰️ Rows hold UTC wall time; the API speaks {@link Instant}. One place converts. */
final class UtcTime {

    private UtcTime() {
    }

    static LocalDateTime now(Clock clock) {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    static LocalDateTime of(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    static Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.toInstant(ZoneOffset.UTC);
    }
}
