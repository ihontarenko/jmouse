package org.jmouse.geometry.taut;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.List;

/**
 * Obstacles as one area a path keeps out of, with the point inside each that tells the ways round it apart. Where a path's own
 * ground is cut out of them — the pads a track starts on —, it runs there whatever stands round it.
 *
 * <p>Its slack is the area grown a little: a path is laid round the slack's edge, so it runs strictly outside the area, never on
 * its edge, where which side it is on would be a matter of rounding.</p>
 *
 * <p>A field is made of what stands still, united once ({@link UnitedObstacles}), and what moves, added each time.</p>
 */
public final class ObstacleField {

    private final GeometryFactory  factory;
    private final Geometry         area;
    private final PreparedGeometry prepared;
    private final Geometry         slack;
    private final List<Coordinate> insides;
    /** How the field's areas are combined: the free room is cut from the window the same way. */
    private final Areas.Grid       grid;

    private ObstacleField(GeometryFactory factory, Geometry area, Geometry slack, List<Coordinate> insides, Areas.Grid grid) {
        this.factory = factory;
        this.grid = grid;
        this.area = area;
        this.prepared = PreparedGeometryFactory.prepare(area);
        this.slack = slack;
        this.insides = List.copyOf(insides);
    }

    /**
     * The field of these obstacles.
     *
     * @param ground where a path may run however near the obstacles: cut out of them; empty for nowhere
     * @param slack  how far outside the area a path is laid round it
     */
    public static ObstacleField of(List<Obstacle> obstacles, Geometry ground, double slack, GeometryFactory factory) {
        return of(UnitedObstacles.none(factory), obstacles, ground, slack, factory);
    }

    /**
     * The field of obstacles united before and of more besides them.
     *
     * @param still  what stands still, united once, grown by the same slack
     * @param moving what is added this time
     * @param ground where a path may run however near the obstacles: cut out of them; empty for nowhere
     * @param slack  how far outside the area a path is laid round it
     */
    public static ObstacleField of(UnitedObstacles still, List<Obstacle> moving, Geometry ground, double slack, GeometryFactory factory) {
        return of(still, moving, ground, slack, factory, Areas.Grid.FLOATING);
    }

    /**
     * The field of obstacles united before and of more besides them, its areas combined on a grid ({@link Areas#onGrid}): what a dense
     * board's near-misses cannot make fail, in any runtime.
     */
    public static ObstacleField of(UnitedObstacles still, List<Obstacle> moving, Geometry ground, double slack, GeometryFactory factory,
                                   Areas.Grid grid) {
        List<Geometry> movingParts = new ArrayList<>();
        moving.forEach(obstacle -> movingParts.addAll(obstacle.parts()));
        Geometry movingArea = grid.unionOf(movingParts, factory);
        Geometry area = united(grid, still.area(), movingArea);
        Geometry grown = united(grid, still.slack(), UnitedObstacles.grown(movingArea, slack));
        boolean grounded = !ground.isEmpty();
        Geometry kept = grounded && !area.isEmpty() ? grid.difference(area, ground) : area;
        Geometry slackArea = grounded && !grown.isEmpty() ? grid.difference(grown, ground) : grown;
        List<Coordinate> insides = new ArrayList<>();
        PreparedGeometry groundPrepared = PreparedGeometryFactory.prepare(ground);
        List<Obstacle> every = new ArrayList<>(still.obstacles());
        every.addAll(moving);
        for (Obstacle obstacle : every) {
            if (!grounded || !groundPrepared.covers(factory.createPoint(obstacle.inside()))) {
                insides.add(obstacle.inside());
                continue;
            }
            // A point cut out with the ground no longer tells anything: one of what is left of its obstacle does.
            Geometry left = grid.difference(grid.unionOf(obstacle.parts(), factory), ground);
            if (!left.isEmpty()) {
                insides.add(left.getInteriorPoint().getCoordinate());
            }
        }
        return new ObstacleField(factory, kept, slackArea, insides, grid);
    }

    private static Geometry united(Areas.Grid grid, Geometry one, Geometry other) {
        if (one.isEmpty()) {
            return other;
        }
        if (other.isEmpty()) {
            return one;
        }
        return grid.union(one, other);
    }

    /** How the field's areas are combined. */
    public Areas.Grid grid() {
        return grid;
    }

    public GeometryFactory factory() {
        return factory;
    }

    /** Where a path may not run. */
    public Geometry area() {
        return area;
    }

    /** The area grown by the slack: what a path is laid round. */
    public Geometry slack() {
        return slack;
    }

    /** A point inside each obstacle. */
    public List<Coordinate> insides() {
        return insides;
    }

    /** Whether a point is inside the area. */
    public boolean blocks(Coordinate point) {
        return prepared.contains(factory.createPoint(point));
    }

    /** The slack's pieces: each a polygon a path may have to be laid round. */
    List<Polygon> slackPieces() {
        List<Polygon> pieces = new ArrayList<>();
        for (int number = 0; number < slack.getNumGeometries(); number++) {
            if (slack.getGeometryN(number) instanceof Polygon polygon && !polygon.isEmpty()) {
                pieces.add(polygon);
            }
        }
        return pieces;
    }
}
