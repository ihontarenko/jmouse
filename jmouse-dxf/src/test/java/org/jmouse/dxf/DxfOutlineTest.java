package org.jmouse.dxf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DxfOutlineTest {

    private static String file(String units, String... entities) {
        StringBuilder text = new StringBuilder("0\nSECTION\n2\nHEADER\n");
        if (units != null) {
            text.append("9\n$INSUNITS\n70\n").append(units).append('\n');
        }
        text.append("0\nENDSEC\n0\nSECTION\n2\nENTITIES\n");
        List.of(entities).forEach(text::append);
        return text.append("0\nENDSEC\n0\nEOF\n").toString();
    }

    private static String line(double x1, double y1, double x2, double y2) {
        return "0\nLINE\n10\n" + x1 + "\n20\n" + y1 + "\n11\n" + x2 + "\n21\n" + y2 + "\n";
    }

    @Test
    void linesInAnyOrderAndWayRoundCloseIntoAnOutline() {
        DxfOutline.Read read = DxfOutline.read(file("4", line(0, 0, 40, 0), line(40, 20, 0, 20), line(40, 0, 40, 20), line(0, 0, 0, 20)));
        assertEquals(1, read.closed().size());
        assertEquals(4, read.closed().getFirst().size());
        assertEquals(0, read.open());
        assertFalse(read.unitless());
    }

    @Test
    void inchesAreMultipliedUp() {
        DxfOutline.Read read = DxfOutline.read(file("1", line(0, 0, 1, 0), line(1, 0, 1, 1), line(1, 1, 0, 0)));
        assertTrue(read.closed().getFirst().stream().anyMatch(point -> point.x() == 25.4));
    }

    @Test
    void aFileSayingNoUnitIsTakenInMillimetresAndSaysSo() {
        assertTrue(DxfOutline.read(file(null, line(0, 0, 1, 0), line(1, 0, 1, 1), line(1, 1, 0, 0))).unitless());
    }

    @Test
    void aCircleIsAnOutlineAndALooseLineIsCountedNotClosed() {
        DxfOutline.Read read = DxfOutline.read(file("4", "0\nCIRCLE\n10\n5\n20\n5\n40\n2\n", line(50, 50, 60, 60), "0\nTEXT\n1\nhello\n"));
        assertEquals(1, read.closed().size());
        assertEquals(1, read.open());
        assertEquals(1, read.skipped());
    }

    @Test
    void aBulgedPolylineWalksItsArc() {
        String rounded = "0\nLWPOLYLINE\n90\n2\n70\n1\n10\n0\n20\n0\n42\n1\n10\n10\n20\n0\n42\n1\n";
        List<DxfOutline.Point> circle = DxfOutline.read(file("4", rounded)).closed().getFirst();
        assertTrue(circle.size() > 20, "a bulge of 1 is a half circle, walked in steps: " + circle.size());
        assertTrue(circle.stream().allMatch(point -> Math.abs(Math.hypot(point.x() - 5, point.y()) - 5) < 1e-6), "every point on the circle");
    }

    @Test
    void whatIsNoDrawingIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> DxfOutline.read("not a dxf\nat all\n"));
    }
}
