package org.jmouse.j2js.teavm.supplement.jdk;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The page's Math answers bit for bit as the JVM's: on values of every magnitude, and on the edges each is defined by. */
class MathSupplementsTest {

    private static final int VALUES = 200_000;

    /** A double of any magnitude and sign, from random bits — the specials among them. */
    private static double anyDouble(SplittableRandom random) {
        return Double.longBitsToDouble(random.nextLong());
    }

    /** A double of the size a pcb measures: millimetres, and their small differences. */
    private static double pcbDouble(SplittableRandom random) {
        return (random.nextDouble() - 0.5) * Math.pow(10, random.nextInt(-9, 4));
    }

    @Test
    void hypotIsTheJvmsBitForBit() {
        SplittableRandom random = new SplittableRandom(7);
        for (int index = 0; index < VALUES; index++) {
            double x = index % 2 == 0 ? anyDouble(random) : pcbDouble(random);
            double y = index % 3 == 0 ? anyDouble(random) : pcbDouble(random);
            assertEquals(Double.doubleToRawLongBits(StrictMath.hypot(x, y)), Double.doubleToRawLongBits(MathSupplements.hypot(x, y)),
                    () -> "hypot(" + x + ", " + y + ")");
        }
        double[] edges = {0.0, -0.0, Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.NaN, 1e-310, 1e300, 3, 4};
        for (double x : edges) {
            for (double y : edges) {
                assertEquals(Double.doubleToLongBits(StrictMath.hypot(x, y)), Double.doubleToLongBits(MathSupplements.hypot(x, y)),
                        () -> "hypot(" + x + ", " + y + ")");
            }
        }
    }

    @Test
    void roundAndRintAreTheJvmsOnHalvesAndEverywhere() {
        double[] edges = {0.5, -0.5, 1.5, -1.5, 2.5, -2.5, 0.49999999999999994, -0.49999999999999994, -0.0, 0.0, 4503599627370497.0,
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 9.3e18, -9.3e18, -0.3, 0.3};
        for (double value : edges) {
            assertEquals(Math.round(value), MathSupplements.round(value), () -> "round(" + value + ")");
            assertEquals(Double.doubleToLongBits(Math.rint(value)), Double.doubleToLongBits(MathSupplements.rint(value)), () -> "rint(" + value + ")");
        }
        SplittableRandom random = new SplittableRandom(11);
        for (int index = 0; index < VALUES; index++) {
            double value = index % 2 == 0 ? pcbDouble(random) * 1e6 : (random.nextInt(-1000, 1000) + 0.5);
            assertEquals(Math.round(value), MathSupplements.round(value), () -> "round(" + value + ")");
            assertEquals(Double.doubleToLongBits(Math.rint(value)), Double.doubleToLongBits(MathSupplements.rint(value)), () -> "rint(" + value + ")");
        }
    }
}
