package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.Supplements;

import java.time.Instant;
import java.util.Date;

/**
 * 🧩 What TeaVM's {@code java.util.Date} lacks: its bridge to {@code java.time}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(Date.class)
public final class DateSupplements {

    private DateSupplements() {
    }

    @Supplement(SupplementKind.STATIC)
    public static Date from(Instant instant) {
        return new Date(instant.toEpochMilli());
    }

    @Supplement(SupplementKind.INSTANCE)
    public static Instant toInstant(Date date) {
        return Instant.ofEpochMilli(date.getTime());
    }
}
