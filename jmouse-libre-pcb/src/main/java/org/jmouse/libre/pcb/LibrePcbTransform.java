package org.jmouse.libre.pcb;

/**
 * How a thing stands where it is placed — a symbol on a sheet, a device on a board —, as LibrePCB's {@code Transform::map}
 * does it: mirrored across the Y axis where it says, then turned counter-clockwise, then moved.
 */
public record LibrePcbTransform(LibrePcbPoint at, double rotation, boolean mirrored) {

    /** Where a point of the thing stands. */
    public LibrePcbPoint placed(LibrePcbPoint point) {
        double mirroredX = mirrored ? -point.x() : point.x();
        double radians = Math.toRadians(rotation);
        return new LibrePcbPoint(at.x() + mirroredX * Math.cos(radians) - point.y() * Math.sin(radians),
                at.y() + mirroredX * Math.sin(radians) + point.y() * Math.cos(radians));
    }

    /** The way a direction of the thing runs once it is placed, itself a direction. */
    public LibrePcbPoint turned(LibrePcbPoint direction) {
        LibrePcbPoint moved = new LibrePcbTransform(new LibrePcbPoint(0, 0), rotation, mirrored).placed(direction);
        return new LibrePcbPoint(moved.x(), moved.y());
    }
}
