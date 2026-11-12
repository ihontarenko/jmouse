package org.jmouse.svg.path.node;

import org.jmouse.svg.path.node.PathSegment.PathPoint;
import org.jmouse.svg.path.parser.PathParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathContoursTest {

    private static final PathParser PATHS = new PathParser();
    private static final double NEAR = 1e-9;

    @Test
    void roundedRectangleIsOneClosedRunOfLinesAndCircularArcs() {
        List<PathContours.PathContour> contours = PathContours.of(PATHS.parse(
                "M1 3H31A1 1 0 0 1 32 4V24A1 1 0 0 1 31 25H1A1 1 0 0 1 0 24V4A1 1 0 0 1 1 3Z"));

        assertEquals(1, contours.size());
        PathContours.PathContour contour = contours.getFirst();
        assertTrue(contour.closed());
        assertEquals(9, contour.segments().size());
        PathSegment.EllipticalArc corner = assertInstanceOf(PathSegment.EllipticalArc.class, contour.segments().get(1));
        assertTrue(corner.circular());
        near(new PathPoint(31, 4), corner.centre());
        PathPoint middle = corner.at(0.5);
        assertEquals(1, Math.hypot(middle.x() - 31, middle.y() - 4), NEAR);
        near(new PathPoint(31, 3), corner.from());
        near(new PathPoint(32, 4), corner.to());
        for (int index = 1; index < contour.segments().size(); index++) {
            near(contour.segments().get(index - 1).to(), contour.segments().get(index).from());
        }
        near(contour.start(), contour.segments().getLast().to());
    }

    @Test
    void relativeCommandsAndSmoothCurvesFollowThePen() {
        List<PathContours.PathContour> contours = PathContours.of(PATHS.parse("m10 10 l5 0 c0 5 5 5 5 0 s5 -5 5 0"));

        PathContours.PathContour contour = contours.getFirst();
        assertFalse(contour.closed());
        PathSegment.Line line = assertInstanceOf(PathSegment.Line.class, contour.segments().get(0));
        near(new PathPoint(15, 10), line.to());
        PathSegment.Cubic smooth = assertInstanceOf(PathSegment.Cubic.class, contour.segments().get(2));
        // The smooth curve's first handle mirrors the last curve's second handle about where they meet.
        near(new PathPoint(20, 5), smooth.first());
        near(new PathPoint(25, 10), smooth.to());
    }

    @Test
    void aMoveStartsANewRunAndALoneMoveDrawsNothing() {
        List<PathContours.PathContour> contours = PathContours.of(PATHS.parse("M0 0 L1 0 M5 5 M7 7 L8 7 Z"));

        assertEquals(2, contours.size());
        near(new PathPoint(7, 7), contours.get(1).start());
        assertTrue(contours.get(1).closed());
    }

    @Test
    void polylinesStepTheSameRuns() {
        List<List<double[]>> polylines = PathPolylines.of(PATHS.parse("M0 0 L10 0 A5 5 0 0 1 10 10 Z"), 4);

        List<double[]> run = polylines.getFirst();
        assertEquals(1 + 1 + 4 + 1, run.size());
        assertEquals(15, run.get(3)[0], NEAR);
        assertEquals(5, run.get(3)[1], NEAR);
        assertEquals(0, run.getLast()[0], NEAR);
        assertEquals(0, run.getLast()[1], NEAR);
    }

    private static void near(PathPoint expected, PathPoint actual) {
        assertEquals(expected.x(), actual.x(), NEAR, "x of " + actual);
        assertEquals(expected.y(), actual.y(), NEAR, "y of " + actual);
    }
}
