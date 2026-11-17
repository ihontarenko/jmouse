package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.time.Instant;
import java.util.Calendar;

/**
 * 🧩 What TeaVM's {@code java.util.Calendar} lacks: its bridge to {@code java.time}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(Calendar.class)
public final class CalendarSupplements {

    private CalendarSupplements() {
    }

    @Supplement(SupplementKind.INSTANCE)
    public static Instant toInstant(Calendar calendar) {
        return Instant.ofEpochMilli(calendar.getTimeInMillis());
    }
}
