package org.jmouse.dxf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DxfDrawingTest {

    private static final DxfDrawing.Point ORIGIN = new DxfDrawing.Point(0, 0);
    private static final DxfDrawing.Point FAR    = new DxfDrawing.Point(10, 5);

    @Test
    void aFileIsPairsOfACodeAndAValue() {
        String[] lines = new DxfDrawing().line("edge", ORIGIN, FAR).write().split("\r\n");
        assertEquals(0, lines.length % 2, "codes and values come in pairs");
        for (int index = 0; index < lines.length; index += 2) {
            assertTrue(lines[index].strip().matches("\\d+"), "line " + index + " is a code: " + lines[index]);
        }
        assertEquals("EOF", lines[lines.length - 1]);
    }

    @Test
    void itSaysItsReleaseItsLayersAndItsEntities() {
        String file = new DxfDrawing().layer("edge", 3).line("edge", ORIGIN, FAR).circle("holes", FAR, 1.5).write();
        assertTrue(file.contains("$ACADVER\r\n  1\r\nAC1009"));
        assertTrue(file.contains("LAYER\r\n  2\r\nedge\r\n 70\r\n0\r\n 62\r\n3"));
        assertTrue(file.contains("LAYER\r\n  2\r\nholes\r\n 70\r\n0\r\n 62\r\n7"), "a layer drawn on without a colour is declared");
        assertTrue(file.contains("LINE\r\n  8\r\nedge\r\n 10\r\n0.000000\r\n 20\r\n0.000000\r\n 11\r\n10.000000\r\n 21\r\n5.000000"));
        assertTrue(file.contains("CIRCLE\r\n  8\r\nholes\r\n 10\r\n10.000000\r\n 20\r\n5.000000\r\n 40\r\n1.500000"));
    }

    @Test
    void aPolylineEndsItsVertices() {
        String file = new DxfDrawing().polyline("edge", List.of(ORIGIN, FAR, ORIGIN), 0.2, true).write();
        assertEquals(3, file.split("VERTEX", -1).length - 1);
        assertTrue(file.contains("SEQEND"));
        assertTrue(file.contains(" 70\r\n1\r\n"), "closed");
    }

    @Test
    void wordsBeyondAsciiAreEscaped() {
        assertTrue(new DxfDrawing().text("notes", ORIGIN, 2, 0, "Я 1\nx").write().contains("\\U+042F 1 x"));
    }

    @Test
    void whatDxfCannotSayIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new DxfDrawing().layer("a/b", 1));
        assertThrows(IllegalArgumentException.class, () -> new DxfDrawing().layer("a", 0));
        assertThrows(IllegalArgumentException.class, () -> new DxfDrawing().polyline("a", List.of(ORIGIN), 0, false));
        assertThrows(IllegalArgumentException.class, () -> new DxfDrawing().solid("a", List.of(ORIGIN, FAR)));
        assertThrows(IllegalArgumentException.class, () -> new DxfDrawing().circle("a", new DxfDrawing.Point(Double.NaN, 0), 1));
    }
}
