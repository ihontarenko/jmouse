package org.jmouse.eagle;

import java.util.List;

/**
 * Something drawn on a layer, in millimetres with Y upwards, as Eagle draws it: a wire — straight, or an arc where it
 * has a {@code curve} —, a circle, a rectangle, a polygon, a text.
 */
public sealed interface EagleGraphic {

    /** The layer it is drawn on, by Eagle's number: 1 top copper, 16 bottom, 20 the board's dimension, 21 top place … */
    int layer();

    /**
     * A wire from a point to a point, as wide as {@code width}; an arc of {@code curve} degrees — counter-clockwise from
     * the first point to the second when positive — where that is not zero.
     */
    record Wire(double x1, double y1, double x2, double y2, double width, int layer, double curve) implements EagleGraphic {
    }

    /** A circle round a point; filled when its {@code width} is zero. */
    record Circle(double x, double y, double radius, double width, int layer) implements EagleGraphic {
    }

    /** A filled rectangle between two corners, turned about its middle. */
    record Rectangle(double x1, double y1, double x2, double y2, int layer, EagleRotation rotation) implements EagleGraphic {
    }

    /** A filled area through its vertices, its outline drawn as wide as {@code width}. */
    record Polygon(List<Vertex> vertices, double width, int layer) implements EagleGraphic {

        public Polygon {
            vertices = List.copyOf(vertices);
        }
    }

    /** A vertex of a polygon; the edge from it to the next is an arc of {@code curve} degrees where that is not zero. */
    record Vertex(double x, double y, double curve) {
    }

    /** A text at a point, as tall as {@code size}. */
    record Text(double x, double y, double size, int layer, EagleRotation rotation, String text) implements EagleGraphic {
    }
}
