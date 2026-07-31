package org.jmouse.helpers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.List;

public final class DateTimeHelper {

    private DateTimeHelper() {}

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
    );

    public static Instant parseInstant(String value) {
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                TemporalAccessor temporal = formatter.parse(value);

                if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
                    return Instant.from(temporal);
                }

                return LocalDateTime.from(temporal)
                        .atZone(ZoneId.systemDefault())
                        .toInstant();
            } catch (Exception ignored) {
            }
        }

        throw new IllegalArgumentException("Unknown date format: " + value);
    }

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
