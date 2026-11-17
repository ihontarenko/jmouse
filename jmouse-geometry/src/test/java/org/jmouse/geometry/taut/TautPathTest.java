package org.jmouse.geometry.taut;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TautPathTest {

    private static final GeometryFactory FACTORY = new GeometryFactory();

    private static final TautPath PULLING = new TautPath(5);

    private static Coordinate at(double x, double y) {
        return new Coordinate(x, y);
    }

    /** A square obstacle, its inside at its middle. */
    private static Obstacle square(double left, double bottom, double size) {
        Polygon area = FACTORY.createPolygon(new Coordinate[] {at(left, bottom), at(left + size, bottom), at(left + size, bottom + size),
                at(left, bottom + size), at(left, bottom)});
        return new Obstacle(area, at(left + size / 2, bottom + size / 2));
    }

    private static ObstacleField field(Obstacle... obstacles) {
        return ObstacleField.of(List.of(obstacles), FACTORY.createPolygon(), 1e-3, FACTORY);
    }

    private static double lengthOf(List<Coordinate> path) {
        return Paths.lengthOf(path);
    }

    @Test
    void aDetourRoundNothingComesStraight() {
        TautPath.Pull pull = PULLING.pulled(List.of(at(0, 0), at(2, 4), at(8, 4), at(10, 0)), field());
        assertEquals(TautPath.Tautness.TAUT, pull.tautness());
        assertEquals(List.of(at(0, 0), at(10, 0)), pull.path());
    }

    @Test
    void aWayRoundAnObstacleStaysOnItsSideAndComesTight() {
        // Over the square, though under it is as short: the side it went is kept.
        TautPath.Pull pull = PULLING.pulled(List.of(at(0, 0), at(0, 6), at(10, 6), at(10, 0)), field(square(4, -1, 2)));
        assertEquals(TautPath.Tautness.TAUT, pull.tautness());
        List<Coordinate> path = pull.path();
        assertEquals(List.of(at(0, 0), at(4, 1), at(6, 1), at(10, 0)), path, "pulled onto the square's upper corners");
    }

    @Test
    void anObstacleMovedOntoAPathPushesItAheadOfItself() {
        // The square came down onto a straight path from above: its inside is above it, so the path is pushed under it.
        TautPath.Pull pull = PULLING.pulled(List.of(at(0, 0), at(10, 0)), field(square(4, -0.5, 2)));
        assertEquals(TautPath.Tautness.TAUT, pull.tautness());
        assertTrue(pull.path().subList(1, pull.path().size() - 1).stream().allMatch(point -> point.y <= -0.5 + 1e-9), "under the square: " + pull.path());
        assertTrue(lengthOf(pull.path()) > 10);
    }

    @Test
    void aPathPushedOntoAnotherObstacleIsSqueezed() {
        // One square came down onto the path, the other stands right under it: no way through, and no going over the first.
        TautPath.Pull pull = PULLING.pulled(List.of(at(0, 0), at(10, 0)), field(square(3, -0.5, 4), square(2, -4.4, 4)));
        assertEquals(TautPath.Tautness.SQUEEZED, pull.tautness());
    }

    @Test
    void aStretchAtOneOfTheEightWaysGetsAnOctagon() {
        Polygon octagon = StretchHull.around(at(0, 0), at(3, 0), 0.5, FACTORY);
        assertEquals(9, octagon.getCoordinates().length);
        assertTrue(octagon.covers(FACTORY.createPoint(at(1.5, 0.49))));
        assertFalse(octagon.covers(FACTORY.createPoint(at(1.5, 0.51))));
    }

    @Test
    void aSlantingStretchsHullRunsAlongItAsCloseAsItIsGrown() {
        // 12 long, 0.3 across: an octagon round it all would be as wide as 0.3 and both grows everywhere.
        Coordinate from = at(0, 0);
        Coordinate to = at(0.3, 12);
        Polygon hull = StretchHull.around(from, to, 0.45, FACTORY);
        double across = Math.hypot(12, 0.3);
        Coordinate middle = at(0.15, 6);
        double normalX = 12 / across;
        double normalY = -0.3 / across;
        assertTrue(hull.covers(FACTORY.createPoint(at(middle.x + normalX * 0.449, middle.y + normalY * 0.449))), "it holds the capsule");
        assertFalse(hull.covers(FACTORY.createPoint(at(middle.x - normalX * 0.5, middle.y - normalY * 0.5))), "nothing much past it");
        assertTrue(hull.covers(FACTORY.createPoint(at(-0.449, 0))));
    }
}
