package org.jmouse.ipc2581;

/**
 * A shape a pad or a flash is drawn with, centred on where it stands — written once into the file's dictionary of
 * standard primitives and named wherever it is used again.
 */
public sealed interface Ipc2581Shape {

    record Circle(double diameter) implements Ipc2581Shape {
    }

    /** A rectangle centred on its point. */
    record RectCenter(double width, double height) implements Ipc2581Shape {
    }

    /** A rectangle centred on its point, every corner rounded by {@code radius}. */
    record RectRound(double width, double height, double radius) implements Ipc2581Shape {
    }

    /** A rectangle whose two shorter ends are half circles. */
    record Oval(double width, double height) implements Ipc2581Shape {
    }

    /** Any other area, its points relative to where it stands. */
    record Contour(Ipc2581Polygon polygon) implements Ipc2581Shape {
    }
}
