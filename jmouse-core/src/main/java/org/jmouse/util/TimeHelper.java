package org.jmouse.util;

import java.time.temporal.ChronoUnit;

public final class TimeHelper {

    private TimeHelper() {}

    public static long toNanos(ChronoUnit unit, long duration) {
        return switch (unit) {
            case NANOS   -> duration;
            case MICROS  -> duration * 1_000L;
            case MILLIS  -> duration * 1_000_000L;
            case SECONDS -> duration * 1_000_000_000L;
            case MINUTES -> duration * 60L * 1_000_000_000L;
            case HOURS   -> duration * 3_600L * 1_000_000_000L;
            case DAYS    -> duration * 86_400L * 1_000_000_000L;
            default -> throw new IllegalArgumentException("UNSUPPORTED UNIT: %s".formatted(unit));
        };
    }

}
