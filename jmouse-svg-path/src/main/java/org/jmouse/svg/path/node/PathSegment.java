package org.jmouse.svg.path.node;

/**
 * One piece of a path as it is drawn, in absolute coordinates: a line, a curve or an arc from where the pen stood to where
 * it ends — what a path becomes where its curves are kept exact, as a geometry kernel takes them, rather than stepped.
 */
public sealed interface PathSegment {

    /** A point of a path, in its own units. */
    record PathPoint(double x, double y) {
    }

    PathPoint from();

    PathPoint to();

    /** The point a share {@code t} of the way along, from 0 at {@link #from()} to 1 at {@link #to()}. */
    PathPoint at(double t);

    record Line(PathPoint from, PathPoint to) implements PathSegment {

        @Override
        public PathPoint at(double t) {
            return new PathPoint(from.x() + (to.x() - from.x()) * t, from.y() + (to.y() - from.y()) * t);
        }
    }

    /** A quadratic Bézier curve: its one handle between its ends. */
    record Quadratic(PathPoint from, PathPoint handle, PathPoint to) implements PathSegment {

        @Override
        public PathPoint at(double t) {
            double u = 1 - t;
            return new PathPoint(u * u * from.x() + 2 * u * t * handle.x() + t * t * to.x(),
                                 u * u * from.y() + 2 * u * t * handle.y() + t * t * to.y());
        }
    }

    /** A cubic Bézier curve: its two handles between its ends. */
    record Cubic(PathPoint from, PathPoint first, PathPoint second, PathPoint to) implements PathSegment {

        @Override
        public PathPoint at(double t) {
            double u = 1 - t;
            return new PathPoint(u * u * u * from.x() + 3 * u * u * t * first.x() + 3 * u * t * t * second.x() + t * t * t * to.x(),
                                 u * u * u * from.y() + 3 * u * u * t * first.y() + 3 * u * t * t * second.y() + t * t * t * to.y());
        }
    }

    /**
     * An arc of an ellipse by its centre, as SVG's arc is found from its ends.
     *
     * @param turn       radians the ellipse's X axis is turned by
     * @param startAngle radians on the ellipse where the arc starts
     * @param span       radians it runs, negative against the way the angle grows
     */
    record EllipticalArc(PathPoint from, PathPoint to, PathPoint centre, double radiusX, double radiusY, double turn,
                         double startAngle, double span) implements PathSegment {

        /** How near two radii may be and the arc still be one of a circle. */
        private static final double SAME_RADIUS = 1e-9;

        /** Whether it is an arc of a circle — every arc a board's edge or a footprint is drawn with. */
        public boolean circular() {
            return Math.abs(radiusX - radiusY) <= SAME_RADIUS * Math.max(1, radiusX);
        }

        @Override
        public PathPoint at(double t) {
            double angle = startAngle + span * t;
            double ellipseX = radiusX * Math.cos(angle);
            double ellipseY = radiusY * Math.sin(angle);
            double cosine = Math.cos(turn);
            double sine = Math.sin(turn);
            return new PathPoint(centre.x() + cosine * ellipseX - sine * ellipseY, centre.y() + sine * ellipseX + cosine * ellipseY);
        }
    }
}
