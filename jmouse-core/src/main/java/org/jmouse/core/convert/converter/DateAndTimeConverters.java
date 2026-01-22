package org.jmouse.core.convert.converter;

import org.jmouse.core.convert.GenericConverter;

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
                of(Instant.class, Instant.class, Instant::from)
        );
    }

}
