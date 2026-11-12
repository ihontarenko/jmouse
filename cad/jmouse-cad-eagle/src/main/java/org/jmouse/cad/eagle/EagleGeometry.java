package org.jmouse.cad.eagle;

import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.eagle.EagleGraphic;
import org.jmouse.eagle.EagleRotation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Eagle's shapes as points a drawing is made of: an arc given by its two ends and its turn as the three points it runs
 * through, a turned rectangle as its four corners, a polygon's edges as its path. Every point is handed through
 * {@code place}, which turns Eagle's millimetres with Y upwards into the drawing's own.
 */
final class EagleGeometry {

    private EagleGeometry() {
    }

    /**
     * The point half way along an arc from one end to the other turning {@code curve} degrees — counter-clockwise when
     * positive —, in Eagle's frame.
     */
    static double[] arcMiddle(double x1, double y1, double x2, double y2, double curve) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        double half = Math.toRadians(curve) / 2;
        // The centre stands off the chord's middle along its left normal, by half the chord over the half turn's tangent.
        double offset = length / 2 / Math.tan(half);
        double centreX = (x1 + x2) / 2 - dy / length * offset;
        double centreY = (y1 + y2) / 2 + dx / length * offset;
        double radius = Math.hypot(x1 - centreX, y1 - centreY);
        double middle = Math.atan2(y1 - centreY, x1 - centreX) + half;
        return new double[] {centreX + radius * Math.cos(middle), centreY + radius * Math.sin(middle)};
    }

    /** A wire drawn into a path: a line, or an arc through its middle — going on from the last where it starts there. */
    static void wire(EagleGraphic.Wire wire, RoundedPath path, BiFunction<Double, Double, double[]> place) {
        double[] from = place.apply(wire.x1(), wire.y1());
        double[] to = place.apply(wire.x2(), wire.y2());
        if (wire.curve() == 0) {
            path.lineFrom(from[0], from[1], to[0], to[1]);
            return;
        }
        double[] middle = arcMiddle(wire.x1(), wire.y1(), wire.x2(), wire.y2(), wire.curve());
        double[] through = place.apply(middle[0], middle[1]);
        path.arcThrough(from[0], from[1], through[0], through[1], to[0], to[1]);
    }

    /** A polygon's outline drawn into a path, closed: each edge straight, or an arc where its vertex says a curve. */
    static void polygon(EagleGraphic.Polygon polygon, RoundedPath path, BiFunction<Double, Double, double[]> place) {
        List<EagleGraphic.Vertex> vertices = polygon.vertices();
        if (vertices.size() < 3) {
            return;
        }
        double[] start = place.apply(vertices.getFirst().x(), vertices.getFirst().y());
        path.moveTo(start[0], start[1]);
        for (int index = 0; index < vertices.size(); index++) {
            EagleGraphic.Vertex from = vertices.get(index);
            EagleGraphic.Vertex to = vertices.get((index + 1) % vertices.size());
            double[] end = place.apply(to.x(), to.y());
            if (from.curve() == 0) {
                path.lineTo(end[0], end[1]);
            } else {
                double[] at = place.apply(from.x(), from.y());
                double[] middle = arcMiddle(from.x(), from.y(), to.x(), to.y(), from.curve());
                double[] through = place.apply(middle[0], middle[1]);
                path.arcThrough(at[0], at[1], through[0], through[1], end[0], end[1]);
            }
        }
        path.close();
    }

    /** A rectangle's four corners, turned about its middle, in Eagle's frame. */
    static List<double[]> corners(EagleGraphic.Rectangle rectangle) {
        double middleX = (rectangle.x1() + rectangle.x2()) / 2;
        double middleY = (rectangle.y1() + rectangle.y2()) / 2;
        double[][] corners = {{rectangle.x1(), rectangle.y1()}, {rectangle.x2(), rectangle.y1()},
                {rectangle.x2(), rectangle.y2()}, {rectangle.x1(), rectangle.y2()}};
        List<double[]> turned = new ArrayList<>();
        double radians = Math.toRadians(rectangle.rotation().angle());
        for (double[] corner : corners) {
            double x = corner[0] - middleX;
            double y = corner[1] - middleY;
            turned.add(new double[] {middleX + x * Math.cos(radians) - y * Math.sin(radians),
                    middleY + x * Math.sin(radians) + y * Math.cos(radians)});
        }
        return turned;
    }

    /** A filled rectangle drawn into a path. */
    static void rectangle(EagleGraphic.Rectangle rectangle, RoundedPath path, BiFunction<Double, Double, double[]> place) {
        List<double[]> corners = corners(rectangle);
        double[] first = place.apply(corners.getFirst()[0], corners.getFirst()[1]);
        path.moveTo(first[0], first[1]);
        for (double[] corner : corners.subList(1, corners.size())) {
            double[] at = place.apply(corner[0], corner[1]);
            path.lineTo(at[0], at[1]);
        }
        path.close();
    }

    /** A direction an Eagle rotation points, in Eagle's frame: {@code R0} is to the right. */
    static double[] direction(EagleRotation rotation) {
        return turned(1, 0, rotation);
    }

    /** A point of a thing as Eagle turns it, in Eagle's frame: turned counter-clockwise, then mirrored about the vertical. */
    static double[] turned(double x, double y, EagleRotation rotation) {
        double radians = Math.toRadians(rotation.angle());
        double turnedX = x * Math.cos(radians) - y * Math.sin(radians);
        double turnedY = x * Math.sin(radians) + y * Math.cos(radians);
        return new double[] {rotation.mirrored() ? -turnedX : turnedX, turnedY};
    }

    /**
     * The jMX turn of a symbol Eagle turns so: jMX mirrors first and then turns clockwise on a sheet whose Y goes down,
     * which comes to the same angle for a mirrored one and its complement for any other.
     */
    static double sheetRotation(EagleRotation rotation) {
        return rotation.mirrored() ? rotation.angle() : (360 - rotation.angle()) % 360;
    }
}
