package org.jmouse.svg.path.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Makes a path as its tree, a command at a time, in absolute coordinates: {@code PathBuilder.path().moveTo(-4, y)
 * .horizontalTo(40).build()}. What it builds writes itself as path data; no caller ever writes the text.
 */
public final class PathBuilder {

    private final List<PathCommand> commands = new ArrayList<>();

    private PathBuilder() {
    }

    public static PathBuilder path() {
        return new PathBuilder();
    }

    public PathBuilder moveTo(double x, double y) {
        return command(PathCommandKind.MOVE, x, y);
    }

    public PathBuilder lineTo(double x, double y) {
        return command(PathCommandKind.LINE, x, y);
    }

    public PathBuilder horizontalTo(double x) {
        return command(PathCommandKind.HORIZONTAL, x);
    }

    public PathBuilder verticalTo(double y) {
        return command(PathCommandKind.VERTICAL, y);
    }

    public PathBuilder cubicTo(double firstX, double firstY, double secondX, double secondY, double x, double y) {
        return command(PathCommandKind.CUBIC, firstX, firstY, secondX, secondY, x, y);
    }

    public PathBuilder quadraticTo(double controlX, double controlY, double x, double y) {
        return command(PathCommandKind.QUADRATIC, controlX, controlY, x, y);
    }

    /** An elliptical arc to (x, y): its radii, the ellipse's rotation, which of the four arcs it is. */
    public PathBuilder arcTo(double radiusX, double radiusY, double rotation, boolean largeArc, boolean sweep, double x, double y) {
        commands.add(new PathCommand(PathCommandKind.ARC, false, true, List.of(PathNumber.of(radiusX), PathNumber.of(radiusY),
                PathNumber.of(rotation), PathNumber.of(largeArc), PathNumber.of(sweep), PathNumber.of(x), PathNumber.of(y))));
        return this;
    }

    public PathBuilder close() {
        return command(PathCommandKind.CLOSE);
    }

    /** The commands of another path, after these. */
    public PathBuilder append(PathData path) {
        commands.addAll(path.commands());
        return this;
    }

    public PathData build() {
        return new PathData(commands);
    }

    private PathBuilder command(PathCommandKind kind, double... numbers) {
        commands.add(new PathCommand(kind, false, true, Arrays.stream(numbers).mapToObj(PathNumber::of).toList()));
        return this;
    }
}
