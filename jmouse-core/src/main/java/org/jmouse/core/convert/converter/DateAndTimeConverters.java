package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.GenericConverter;
import org.jmouse.helpers.DateTimeHelper;

import java.time.*;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Set;

import static org.jmouse.core.convert.GenericConverter.of;

public class DateAndTimeConverters {

    public static Set<GenericConverter<?, ?>> getConverters() {
        return Set.of(
                of(Integer.class, Instant.class, Instant::ofEpochSecond),
                of(Instant.class, LocalDateTime.class, source
                        -> LocalDateTime.ofInstant(source, ZoneId.systemDefault())),
                of(LocalDateTime.class, Instant.class, source
                        -> source.atZone(ZoneId.systemDefault()).toInstant()),
                of(Date.class, Instant.class, Date::toInstant),
                of(Instant.class, Date.class, Date::from),
                of(String.class, LocalDate.class, LocalDate::parse),
                of(LocalDate.class, String.class, LocalDate::toString),
                of(String.class, ZonedDateTime.class, ZonedDateTime::parse),
                of(ZonedDateTime.class, String.class, ZonedDateTime::toString),
                of(GregorianCalendar.class, Instant.class, GregorianCalendar::toInstant),
                of(Instant.class, GregorianCalendar.class, source
                        -> GregorianCalendar.from(source.atZone(ZoneId.systemDefault()))),
                of(Instant.class, Instant.class, Instant::from),
                of(String.class, Instant.class, source -> {
                    String value = source.trim();

                    if (value.matches("^-?\\d{13}$")) {
                        return Instant.ofEpochMilli(Long.parseLong(value));
                    }

                    if (value.matches("^-?\\d+$")) {
                        return Instant.ofEpochSecond(Long.parseLong(value));
                    }

                    return DateTimeHelper.parseInstant(value);
                }),
                of(Instant.class, String.class, source -> {
                    // ISO-8601 in UTC, e.g. "2026-01-29T16:45:12.345Z"
                    return source.toString();
                }),

                // Amounts of time, in the JDK's own notation: "PT15M", "PT1H", "P365D". Without
                // these a Duration or Period component simply cannot be bound — the binder falls
                // back to treating it as a value object and looks for a constructor that does not
                // exist, which reads as a confusing reflection failure rather than as a missing
                // converter. Framework-specific shorthand ("15m") is deliberately not accepted
                // here: that is a convention of whoever wrote the configuration file, and belongs
                // in the adapter that reads it.
                of(String.class, Duration.class, source -> Duration.parse(source.trim())),
                of(Duration.class, String.class, Duration::toString),
                of(String.class, Period.class, source -> Period.parse(source.trim())),
                of(Period.class, String.class, Period::toString)
        );
    }

}
