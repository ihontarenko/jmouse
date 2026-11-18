package org.jmouse.geometry.taut;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.List;

/**
 * Obstacles united once — their area and the area grown by the slack — to be laid among again and again: what stands still while
 * a path is pulled step after step is united a single time, and only what moves is added to it each step ({@link ObstacleField}).
 *
 * @param area      the obstacles' parts as one area
 * @param slack     the area grown by the slack
 * @param obstacles the obstacles, each with its parts and the point that tells the ways round it apart
 */
public record UnitedObstacles(Geometry area, Geometry slack, List<Obstacle> obstacles) {

    public UnitedObstacles {
        obstacles = List.copyOf(obstacles);
    }

    /** These obstacles united, their area grown by a slack. */
    public static UnitedObstacles of(List<Obstacle> obstacles, double slack, GeometryFactory factory) {
        return of(obstacles, slack, factory, Areas.Grid.FLOATING);
    }

    /** These obstacles united on a grid ({@link Areas#onGrid}), their area grown by a slack. */
    public static UnitedObstacles of(List<Obstacle> obstacles, double slack, GeometryFactory factory, Areas.Grid grid) {
        List<Geometry> parts = new ArrayList<>();
        for (Obstacle obstacle : obstacles) {
            parts.addAll(obstacle.parts());
        }
        Geometry area = grid.unionOf(parts, factory);
        return new UnitedObstacles(area, grown(area, slack), obstacles);
    }

    /** None at all. */
    public static UnitedObstacles none(GeometryFactory factory) {
        return new UnitedObstacles(factory.createPolygon(), factory.createPolygon(), List.of());
    }

    /** An area grown by a slack, its corners kept sharp as an octagon's are. */
    static Geometry grown(Geometry area, double slack) {
        return area.isEmpty() ? area : area.buffer(slack, 1, BufferParameters.CAP_FLAT);
    }
}
