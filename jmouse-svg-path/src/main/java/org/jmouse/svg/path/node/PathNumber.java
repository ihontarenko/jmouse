package org.jmouse.svg.path.node;

import java.math.BigDecimal;

/**
 * A number of a path: its value, and how it is written — as its author wrote it when it was read ({@code .5},
 * {@code 2.50}), in as few digits as read back the same when a builder made it.
 *
 * @param joined whether it is written against what comes before it, with nothing between — {@code -24-26}, as an author
 *               may; a number a builder made never is
 */
public record PathNumber(double value, String written, boolean joined) {

    public static PathNumber of(double value) {
        if (value == 0) {
            return new PathNumber(0, "0", false);
        }
        return new PathNumber(value, BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(), false);
    }

    /** An arc's flag: {@code 1} or {@code 0}. */
    public static PathNumber of(boolean flag) {
        return of(flag ? 1 : 0);
    }

    @Override
    public String toString() {
        return written;
    }
}
