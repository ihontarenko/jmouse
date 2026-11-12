package org.jmouse.altium;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A length or an angle as Altium writes one in a property: {@code 19.685mil}, {@code 0.5mm}, {@code 2.7E+0002} — read
 * once here, by the reading and by whatever converts what it read.
 */
public final class AltiumMeasures {

    /** A length: a number, then its unit, mils when it names none. */
    private static final Pattern LENGTH = Pattern.compile("\s*(-?[0-9.]+(?:[eE][-+]?[0-9]+)?)\s*(mil|mm)?\s*");

    /** How many of Altium's units make a mil. */
    private static final double UNITS_PER_MIL = 10_000;

    private AltiumMeasures() {
    }

    /**
     * A length a property writes, in Altium's units; zero for none.
     *
     * @throws AltiumReadException for a length Altium does not write so
     */
    public static double units(String written) {
        if (written == null || written.isBlank()) {
            return 0;
        }
        Matcher matcher = LENGTH.matcher(written);
        if (!matcher.matches()) {
            throw new AltiumReadException("'" + written + "' is no length Altium writes.");
        }
        double value = Double.parseDouble(matcher.group(1));
        return "mm".equals(matcher.group(2)) ? value * AltiumPcbLibrary.UNITS_PER_MILLIMETRE : value * UNITS_PER_MIL;
    }

    /** An angle a property writes, in degrees; zero for none. */
    public static double degrees(String written) {
        return written == null || written.isBlank() ? 0 : Double.parseDouble(written.strip());
    }
}
