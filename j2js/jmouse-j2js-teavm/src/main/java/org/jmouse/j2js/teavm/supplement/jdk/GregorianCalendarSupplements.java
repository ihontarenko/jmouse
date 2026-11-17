package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * 🧩 What TeaVM's {@code java.util.GregorianCalendar} lacks: its bridge from {@code java.time}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(GregorianCalendar.class)
public final class GregorianCalendarSupplements {

    private GregorianCalendarSupplements() {
    }

    /** The calendar of a zoned date and time, with ISO 8601's week — Monday first, four days in the first week — as the JDK's. */
    @Supplement(SupplementKind.STATIC)
    public static GregorianCalendar from(ZonedDateTime dateTime) {
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(dateTime.getZone().getId()));
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setMinimalDaysInFirstWeek(4);
        calendar.setTimeInMillis(dateTime.toInstant().toEpochMilli());
        return calendar;
    }
}
