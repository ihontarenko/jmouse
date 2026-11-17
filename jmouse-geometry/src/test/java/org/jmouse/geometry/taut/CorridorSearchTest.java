package org.jmouse.geometry.taut;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorridorSearchTest {

    private static final GeometryFactory FACTORY = new GeometryFactory();

    private static final Envelope WINDOW = new Envelope(-60, 60, -60, 60);

    private static Coordinate at(double x, double y) {
        return new Coordinate(x, y);
    }

    private static Obstacle box(double left, double bottom, double right, double top) {
        Polygon area = FACTORY.createPolygon(new Coordinate[] {at(left, bottom), at(right, bottom), at(right, top), at(left, top), at(left, bottom)});
        return new Obstacle(area, at((left + right) / 2, (bottom + top) / 2));
    }

    private static ObstacleField field(Obstacle... obstacles) {
        return ObstacleField.of(List.of(obstacles), FACTORY.createPolygon(), 1e-3, FACTORY);
    }

    @Test
    void aWayWithNothingInItIsTheStraightLine() {
        assertEquals(Optional.of(List.of(at(-10, 0), at(10, 0))), CorridorSearch.way(field(), WINDOW, at(-10, 0), at(10, 0)));
    }

    /** A wall across the straight way with its only gap far off to one side: the way goes all the way round through it, tight on its corners. */
    @Test
    void aWayGoesFarRoundAWallThroughItsOnlyGapTightOnItsCorners() {
        ObstacleField wall = field(box(-1, -60, 1, 30), box(-1, 34, 1, 60));
        List<Coordinate> way = CorridorSearch.way(wall, WINDOW, at(-10, 0), at(10, 0)).orElseThrow();
        assertEquals(List.of(at(-10, 0), at(-1, 30), at(1, 30), at(10, 0)), way);
    }

    @Test
    void noWayWhereTheWallIsWhole() {
        assertTrue(CorridorSearch.way(field(box(-1, -60, 1, 60)), WINDOW, at(-10, 0), at(10, 0)).isEmpty());
    }

    /** Where a hop is allowed — anywhere off the wall —, through to the layer beyond. */
    private static CorridorSearch.Hops<String> throughAnywhere(double cost) {
        return (layer, at) -> List.of(new CorridorSearch.Hop<>(at, 1 - layer, at, cost, "via"));
    }

    /**
     * A whole wall on the start's layer, nothing on the other: the way runs to a hop, over to the other layer, and on to the end
     * there — the hops are the asker's, their points the triangles' middles.
     */
    @Test
    void aWayOverLayersHopsWhereItsOwnLayerIsWalledOff() {
        List<CorridorSearch.Layer> layers = List.of(new CorridorSearch.Layer(field(box(-1, -60, 1, 60)), CorridorSearch.StepCost.LENGTH),
                new CorridorSearch.Layer(field(), CorridorSearch.StepCost.LENGTH));
        List<CorridorSearch.Part<String>> parts = CorridorSearch.searched(new CorridorSearch.Request<>(layers, WINDOW, at(-10, 0), at(10, 0),
                Set.of(0), Set.of(0), throughAnywhere(1))).parts().orElseThrow();
        assertEquals(5, parts.size(), "over and back: " + parts);
        CorridorSearch.Run<String> first = assertInstanceOf(CorridorSearch.Run.class, parts.get(0));
        CorridorSearch.Run<String> over = assertInstanceOf(CorridorSearch.Run.class, parts.get(2));
        CorridorSearch.Run<String> last = assertInstanceOf(CorridorSearch.Run.class, parts.get(4));
        assertEquals(List.of(0, 1, 0), List.of(first.layer(), over.layer(), last.layer()));
        assertEquals(at(-10, 0), first.points().getFirst());
        assertEquals(at(10, 0), last.points().getLast());
        assertTrue(over.points().getFirst().x < -1 && over.points().getLast().x > 1, "across the wall on the other layer: " + over.points());
    }

    /** Where the end stands on both layers, the way hops once and stays on the free one. */
    @Test
    void aWayEndsOnWhicheverLayerTheEndStandsOn() {
        List<CorridorSearch.Layer> layers = List.of(new CorridorSearch.Layer(field(box(-1, -60, 1, 60)), CorridorSearch.StepCost.LENGTH),
                new CorridorSearch.Layer(field(), CorridorSearch.StepCost.LENGTH));
        List<CorridorSearch.Part<String>> parts = CorridorSearch.searched(new CorridorSearch.Request<>(layers, WINDOW, at(-10, 0), at(10, 0),
                Set.of(0), Set.of(0, 1), throughAnywhere(1))).parts().orElseThrow();
        assertEquals(3, parts.size(), "one hop: " + parts);
        assertEquals(1, assertInstanceOf(CorridorSearch.Run.class, parts.get(2)).layer());
    }

    /** A stretch costed more against a layer's way: the way takes the other layer where that is cheaper, though it hops. */
    @Test
    void aStepCostSteersTheWayToTheCheaperLayer() {
        CorridorSearch.StepCost dear = (from, to) -> from.distance(to) * 10;
        List<CorridorSearch.Layer> layers = List.of(new CorridorSearch.Layer(field(), dear), new CorridorSearch.Layer(field(), CorridorSearch.StepCost.LENGTH));
        List<CorridorSearch.Part<String>> parts = CorridorSearch.searched(new CorridorSearch.Request<>(layers, WINDOW, at(-40, 0), at(40, 0),
                Set.of(0, 1), Set.of(0, 1), throughAnywhere(1))).parts().orElseThrow();
        assertEquals(List.of(1), parts.stream().map(part -> ((CorridorSearch.Run<String>) part).layer()).toList(), "along the cheaper layer alone: " + parts);
    }

    /**
     * A wall across the window: no way within it, but the room the start reaches runs to its edge — a larger window may go round.
     * A start walled in on every side: no larger window finds a way either.
     */
    @Test
    void noWayTellsWhetherALargerWindowMayFindOne() {
        assertInstanceOf(CorridorSearch.CutOffByTheWindow.class,
                CorridorSearch.searched(CorridorSearch.alone(field(box(-1, -60, 1, 60)), WINDOW, at(-10, 0), at(10, 0))));
        ObstacleField walledIn = field(box(-15, -5, -5, -4), box(-15, 4, -5, 5), box(-15, -5, -14, 5), box(-6, -5, -5, 5));
        assertInstanceOf(CorridorSearch.ClosedIn.class, CorridorSearch.searched(CorridorSearch.alone(walledIn, WINDOW, at(-10, 0), at(10, 0))));
    }
}
