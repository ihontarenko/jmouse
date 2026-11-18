package org.jmouse.geometry;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SegmentsTest {

    @Test
    void aPointIsNearestTheFootOfItsPerpendicular() {
        assertEquals(0.25, Segments.shareNearest(0, 0, 4, 0, 1, 3), 1e-12);
        assertEquals(0, Segments.shareNearest(0, 0, 4, 0, -2, 1), 1e-12);
        assertEquals(1, Segments.shareNearest(0, 0, 4, 0, 9, 1), 1e-12);
        assertEquals(0, Segments.shareNearest(2, 2, 2, 2, 5, 5), 1e-12);
    }

    @Test
    void crossingSegmentsAreNearestWhereTheyCross() {
        Segments.Shares shares = Segments.nearestShares(0, 0, 4, 4, 0, 4, 4, 0);
        assertEquals(0.5, shares.first(), 1e-12);
        assertEquals(0.5, shares.second(), 1e-12);
    }

    @Test
    void theNearestSharesAreNoFartherThanAnyOthers() {
        Random random = new Random(3);
        for (int ask = 0; ask < 2000; ask++) {
            double[] at = random.doubles(8, -10, 10).toArray();
            Segments.Shares shares = Segments.nearestShares(at[0], at[1], at[2], at[3], at[4], at[5], at[6], at[7]);
            double found = apart(at, shares.first(), shares.second());
            for (int first = 0; first <= 20; first++) {
                for (int second = 0; second <= 20; second++) {
                    assertTrue(found <= apart(at, first / 20.0, second / 20.0) + 1e-9, "not the nearest at ask " + ask);
                }
            }
        }
    }

    private static double apart(double[] at, double first, double second) {
        double firstX = at[0] + (at[2] - at[0]) * first;
        double firstY = at[1] + (at[3] - at[1]) * first;
        double secondX = at[4] + (at[6] - at[4]) * second;
        double secondY = at[5] + (at[7] - at[5]) * second;
        return Math.hypot(firstX - secondX, firstY - secondY);
    }
}
