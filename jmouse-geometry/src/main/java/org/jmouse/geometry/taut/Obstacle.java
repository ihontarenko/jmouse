package org.jmouse.geometry.taut;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.util.List;

/**
 * Something a path keeps out of: the parts of its area, and one point that tells the two ways round it apart — a path passes an
 * obstacle on the side its point is not. The point moves with the obstacle, never farther in one step than the obstacle keeps a
 * path off: a pad's corner as the pad goes, a moving line's end as its pad takes it — never a corner of a line pulled tight again,
 * which may jump.
 *
 * @param parts  where a path may not run, in as many pieces as it comes in
 * @param inside the point that tells the ways round it apart
 */
public record Obstacle(List<Polygon> parts, Coordinate inside) {

    public Obstacle {
        parts = List.copyOf(parts);
    }

    /** An obstacle of one piece. */
    public Obstacle(Polygon area, Coordinate inside) {
        this(List.of(area), inside);
    }
}
