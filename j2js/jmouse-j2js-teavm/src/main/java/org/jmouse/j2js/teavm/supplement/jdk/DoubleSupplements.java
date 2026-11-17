package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.SupplementPlace;
import org.jmouse.j2js.teavm.supplement.Supplements;
import org.teavm.jso.JSBody;

/**
 * 🧩 {@code java.lang.Double} written by the page itself. TeaVM's {@code Double.toString} finds the shortest digits on
 * {@code long}s, a type JavaScript does not have — each a pair of numbers, every division a routine —, so a number written into
 * JSON cost more than the work that found it. JavaScript's own {@code String(number)} finds the same digits natively; they are
 * laid out as Java lays them ({@link DoubleTexts}).
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(Double.class)
public final class DoubleSupplements {

    private DoubleSupplements() {
    }

    /** A double's text, exactly as Java writes it. */
    @Supplement(value = SupplementKind.STATIC, place = SupplementPlace.BODY)
    public static String toString(double number) {
        if (Double.isNaN(number)) {
            return "NaN";
        }
        if (number == Double.POSITIVE_INFINITY) {
            return "Infinity";
        }
        if (number == Double.NEGATIVE_INFINITY) {
            return "-Infinity";
        }
        if (number == 0) {
            // JavaScript writes either zero as 0; Java tells them apart.
            return 1 / number < 0 ? "-0.0" : "0.0";
        }
        return DoubleTexts.javaText(shortest(number));
    }

    /**
     * JavaScript's shortest digits of a finite number, as Java chooses them: of at least two digits — a number one digit says
     * is written with the two nearest it, {@code 4.9E-324} for the least double, not {@code 5e-324}; for any normal double those
     * two are the one digit and a zero.
     */
    @JSBody(params = "number", script = "var text = String(number);"
            + "var digits = text.replace(/^-|e.*$|\\./g, '').replace(/^0+/, '').replace(/0+$/, '');"
            + "return digits.length < 2 ? number.toPrecision(2) : text;")
    private static native String shortest(double number);
}
