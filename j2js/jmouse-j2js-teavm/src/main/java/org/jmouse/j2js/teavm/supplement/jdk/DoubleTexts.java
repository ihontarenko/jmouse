package org.jmouse.j2js.teavm.supplement.jdk;

/**
 * 🔢 A double's text as Java writes it ({@link Double#toString(double)}), from its shortest digits as JavaScript writes them
 * ({@code String(number)}): both choose the same digits — the fewest that read back as the double, the nearest of those —, and
 * differ only in how they lay them out — Java choosing among two digits at least, which its caller asks JavaScript for where
 * one would do. Java writes a number from a thousandth to ten million plainly, with at least one digit
 * after the point ({@code 100.0}); any other in computerized scientific notation, {@code 1.0E-5}. JavaScript writes plainly from
 * a millionth to 10<sup>21</sup>, with no point for a whole number, else {@code 1e-7}, {@code 1.5e+21}.
 *
 * <p>Only a finite number other than zero is laid out here: zero's sign, not a number and the infinities are the caller's.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class DoubleTexts {

    /** The least point Java writes plainly — a thousandth is 0.1 × 10^-2 —, and the most: just under ten million, 0.9… × 10^7. */
    private static final int PLAIN_FROM = -2;
    private static final int PLAIN_TO   = 7;

    private DoubleTexts() {
    }

    /** Java's text of the finite, nonzero number JavaScript wrote as {@code shortest}. */
    public static String javaText(String shortest) {
        boolean negative = shortest.charAt(0) == '-';
        int     start    = negative ? 1 : 0;
        int     marker   = indexOfExponent(shortest);
        int     end      = marker < 0 ? shortest.length() : marker;
        int     exponent = marker < 0 ? 0 : Integer.parseInt(shortest.substring(marker + 1).replace("+", ""));

        StringBuilder digits = new StringBuilder(end - start);
        int           whole  = -1;
        for (int index = start; index < end; index++) {
            char character = shortest.charAt(index);
            if (character == '.') {
                whole = digits.length();
            } else {
                digits.append(character);
            }
        }
        // The number is 0.<digits> × 10^point: the point stands after the whole digits, moved by the exponent.
        int point = (whole < 0 ? digits.length() : whole) + exponent;

        int leading = 0;
        while (leading < digits.length() - 1 && digits.charAt(leading) == '0') {
            leading++;
        }
        int trailing = digits.length();
        while (trailing > leading + 1 && digits.charAt(trailing - 1) == '0') {
            trailing--;
        }
        String significant = digits.substring(leading, trailing);
        point -= leading;

        StringBuilder text = new StringBuilder(significant.length() + 8);
        if (negative) {
            text.append('-');
        }
        if (point >= PLAIN_FROM && point <= PLAIN_TO) {
            plain(text, significant, point);
        } else {
            scientific(text, significant, point);
        }
        return text.toString();
    }

    /** {@code 0.00123}, {@code 120.0}, {@code 12.5}: at least one digit each side of the point. */
    private static void plain(StringBuilder text, String significant, int point) {
        if (point <= 0) {
            text.append("0.").append("0".repeat(-point)).append(significant);
        } else if (point >= significant.length()) {
            text.append(significant).append("0".repeat(point - significant.length())).append(".0");
        } else {
            text.append(significant, 0, point).append('.').append(significant, point, significant.length());
        }
    }

    /** {@code 1.25E-7}, {@code 1.0E21}: one digit before the point, at least one after, then the exponent. */
    private static void scientific(StringBuilder text, String significant, int point) {
        text.append(significant.charAt(0)).append('.');
        if (significant.length() > 1) {
            text.append(significant, 1, significant.length());
        } else {
            text.append('0');
        }
        text.append('E').append(point - 1);
    }

    private static int indexOfExponent(String shortest) {
        int lower = shortest.indexOf('e');
        return lower >= 0 ? lower : shortest.indexOf('E');
    }
}
