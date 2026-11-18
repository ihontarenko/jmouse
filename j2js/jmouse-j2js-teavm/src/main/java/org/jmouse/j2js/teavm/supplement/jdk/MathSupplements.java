package org.jmouse.j2js.teavm.supplement.jdk;

import org.jmouse.j2js.teavm.supplement.Supplement;
import org.jmouse.j2js.teavm.supplement.SupplementKind;
import org.jmouse.j2js.teavm.supplement.SupplementPlace;
import org.jmouse.j2js.teavm.supplement.Supplements;

/**
 * 🧩 {@code java.lang.Math} as Java defines it, where TeaVM's answers otherwise: its {@code hypot} is {@code sqrt(x² + y²)}, its
 * {@code round} adds half the sign and cuts toward zero, its {@code rint} rounds a half away from zero. Each differs from the JVM
 * in the last bits or at the halves — and a router that measures every gap with {@code hypot} decides near a rule differently in the
 * page than on the server, drawing obstacles JTS then finds invalid. These answer exactly as the JVM does: {@code hypot} as fdlibm's
 * {@code e_hypot.c}, the algorithm {@code StrictMath.hypot} is, {@code round} and {@code rint} by their definitions.
 *
 * <p>The fdlibm algorithm is Sun's, freely granted: «Permission to use, copy, modify, and distribute this software is freely granted,
 * provided that this notice is preserved.»</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Supplements(Math.class)
public final class MathSupplements {

    /** The high word of a double whose exponent is that of 2^500: past it, the operands are scaled down. */
    private static final int HIGH_PAST_TWO_TO_500 = 0x5f300000;

    /** The high word of infinity and not a number. */
    private static final int HIGH_INFINITE = 0x7ff00000;

    /** The high word of a double below 2^-500: under it, the operands are scaled up. */
    private static final int HIGH_UNDER_TWO_TO_MINUS_500 = 0x20b00000;

    /** The high word of the largest subnormal double. */
    private static final int HIGH_SUBNORMAL = 0x000fffff;

    /** The high-word step of a scaling by 2^600. */
    private static final int SCALE_600 = 0x25800000;

    /** The high word of 2^1022. */
    private static final int HIGH_TWO_TO_1022 = 0x7fd00000;

    /** How far apart two high words are when one operand is 2^60 times the other: the smaller one no longer counts. */
    private static final int APART_TWO_TO_60 = 0x3c00000;

    /** The high-word step of one in the exponent. */
    private static final int EXPONENT_ONE = 0x00100000;

    /** The high-word mask of a double's magnitude. */
    private static final int MAGNITUDE = 0x7fffffff;

    /** The low twenty bits of a high word: its share of the mantissa. */
    private static final int HIGH_MANTISSA = 0xfffff;

    /** The places the exponent stands shifted by in a high word. */
    private static final int EXPONENT_SHIFT = 20;

    /** 2^52: every double this large or larger is a whole number. */
    private static final double TWO_TO_52 = 4503599627370496.0;

    private MathSupplements() {
    }

    /** {@code sqrt(x² + y²)} with no overflow or underflow on the way, exactly as {@code StrictMath.hypot}: fdlibm's {@code e_hypot.c}. */
    @Supplement(value = SupplementKind.STATIC, place = SupplementPlace.BODY)
    public static double hypot(double x, double y) {
        int high = highOf(x) & MAGNITUDE;
        int low = highOf(y) & MAGNITUDE;
        double a;
        double b;
        if (low > high) {
            a = y;
            b = x;
            int swapped = high;
            high = low;
            low = swapped;
        } else {
            a = x;
            b = y;
        }
        a = withHigh(a, high);
        b = withHigh(b, low);
        if (high - low > APART_TWO_TO_60) {
            return a + b;
        }
        int scaled = 0;
        if (high > HIGH_PAST_TWO_TO_500) {
            if (high >= HIGH_INFINITE) {
                double either = a + b;
                if (((high & HIGH_MANTISSA) | lowOf(a)) == 0) {
                    either = a;
                }
                if (((low ^ HIGH_INFINITE) | lowOf(b)) == 0) {
                    either = b;
                }
                return either;
            }
            high -= SCALE_600;
            low -= SCALE_600;
            scaled += 600;
            a = withHigh(a, high);
            b = withHigh(b, low);
        }
        if (low < HIGH_UNDER_TWO_TO_MINUS_500) {
            if (low <= HIGH_SUBNORMAL) {
                if ((low | lowOf(b)) == 0) {
                    return a;
                }
                double twoTo1022 = fromHigh(HIGH_TWO_TO_1022);
                b *= twoTo1022;
                a *= twoTo1022;
                scaled -= 1022;
            } else {
                high += SCALE_600;
                low += SCALE_600;
                scaled -= 600;
                a = withHigh(a, high);
                b = withHigh(b, low);
            }
        }
        double length;
        double apart = a - b;
        if (apart > b) {
            double t1 = fromHigh(high);
            double t2 = a - t1;
            length = Math.sqrt(t1 * t1 - (b * (-b) - t2 * (a + t1)));
        } else {
            double twice = a + a;
            double y1 = fromHigh(low);
            double y2 = b - y1;
            double t1 = fromHigh(high + EXPONENT_ONE);
            double t2 = twice - t1;
            length = Math.sqrt(t1 * y1 - (apart * (-apart) - (t1 * y2 + t2 * b)));
        }
        if (scaled != 0) {
            return withHigh(1.0, highOf(1.0) + (scaled << EXPONENT_SHIFT)) * length;
        }
        return length;
    }

    /** The whole number nearest, a half up: {@code floor(x + 1/2)} worked out exactly; 0 for not a number, the bounds of a long past them. */
    @Supplement(value = SupplementKind.STATIC, place = SupplementPlace.BODY)
    public static long round(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        if (value >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        if (value <= Long.MIN_VALUE) {
            return Long.MIN_VALUE;
        }
        double floor = Math.floor(value);
        // value - floor is exact below 2^52, so a value a hair under a half is never carried over it as value + 0.5 would be.
        return (long) floor + (value - floor >= 0.5 ? 1 : 0);
    }

    /** The whole number nearest, a half to the even one, the sign of a zero kept. */
    @Supplement(value = SupplementKind.STATIC, place = SupplementPlace.BODY)
    public static double rint(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || Math.abs(value) >= TWO_TO_52) {
            return value;
        }
        double floor = Math.floor(value);
        double apart = value - floor;
        double nearest;
        if (apart < 0.5) {
            nearest = floor;
        } else if (apart > 0.5) {
            nearest = floor + 1;
        } else {
            nearest = floor % 2 == 0 ? floor : floor + 1;
        }
        return nearest == 0 ? Math.copySign(0.0, value) : nearest;
    }

    private static int highOf(double value) {
        return (int) (Double.doubleToRawLongBits(value) >>> 32);
    }

    private static int lowOf(double value) {
        return (int) Double.doubleToRawLongBits(value);
    }

    /** A double with its high word set, its low word kept. */
    private static double withHigh(double value, int high) {
        return Double.longBitsToDouble(((long) high << 32) | (Double.doubleToRawLongBits(value) & 0xffffffffL));
    }

    /** A double of this high word and a low word of nothing. */
    private static double fromHigh(int high) {
        return Double.longBitsToDouble((long) high << 32);
    }
}
