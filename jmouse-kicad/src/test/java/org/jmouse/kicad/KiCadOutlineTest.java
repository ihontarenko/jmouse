package org.jmouse.kicad;

import org.jmouse.sexpression.SExpressionParser;
import org.jmouse.sexpression.node.SExpressionList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** An outline as KiCad 7 and later write a zone's: corners and arcs in one run, or arcs alone where every corner is rounded. */
class KiCadOutlineTest {

    private static KiCadOutline read(String written) {
        SExpressionList points = new SExpressionParser().parse("outline", written).root();
        return KiCadOutline.of(points);
    }

    @Test
    void cornersAndArcsAreReadInTheOrderTheOutlineRuns() {
        KiCadOutline outline = read("(pts (xy 0 0) (arc (start 10 0) (mid 11 1) (end 12 2)) (xy 12 10))");
        assertEquals(3, outline.steps().size());
        assertInstanceOf(KiCadOutline.Corner.class, outline.steps().getFirst());
        assertInstanceOf(KiCadOutline.Bend.class, outline.steps().get(1));
        assertEquals(new KiCadPoint(0, 0), outline.start());
    }

    @Test
    void anOutlineOfArcsAloneIsAnOutlineAndStartsWhereItsFirstArcDoes() {
        KiCadOutline outline = read("(pts (arc (start 1 0) (mid 1.7 0.3) (end 2 1)) (arc (start 2 3) (mid 1.7 3.7) (end 1 4)))");
        assertEquals(2, outline.steps().size());
        assertEquals(new KiCadPoint(1, 0), outline.start());
    }
}
