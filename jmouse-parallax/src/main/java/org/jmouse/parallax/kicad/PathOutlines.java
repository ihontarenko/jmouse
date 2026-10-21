package org.jmouse.parallax.kicad;

import java.util.ArrayList;
import java.util.List;

/**
 * The lines an SVG path's data draws, as polylines: what a symbol's drawing becomes in a KiCad library symbol, which
 * draws polylines. Every command is read — {@code M L H V C S Q T A Z}, absolute and relative —; a curve or an arc is
 * followed by short straight steps, enough that a symbol's small curves read as curves.
 */
final class PathOutlines {

    /** How many straight steps follow a curve or an arc. */
    private static final int STEPS = 8;

    private final String data;
    private int position;

    private PathOutlines(String data) {
        this.data = data;
    }

    /** The polylines a path draws, each a list of points {x, y}; a closed one ends where it began. */
    static List<List<double[]>> of(String data) {
        return new PathOutlines(data == null ? "" : data).read();
    }

    private List<List<double[]>> read() {
        List<List<double[]>> outlines = new ArrayList<>();
        List<double[]> current = null;
        double x = 0;
        double y = 0;
        double startX = 0;
        double startY = 0;
        // The second control point of the last curve, for the smooth curves that mirror it.
        double controlX = 0;
        double controlY = 0;
        char command = ' ';
        char previous = ' ';
        while (true) {
            skipSeparators();
            if (position >= data.length()) {
                break;
            }
            char next = data.charAt(position);
            if (Character.isLetter(next)) {
                command = next;
                position++;
            } else if (command == 'M') {
                command = 'L';
            } else if (command == 'm') {
                command = 'l';
            } else if (command == ' ') {
                throw new IllegalArgumentException("A path begins with a command, not '" + next + "': " + data);
            }
            boolean relative = Character.isLowerCase(command);
            double baseX = relative ? x : 0;
            double baseY = relative ? y : 0;
            switch (Character.toUpperCase(command)) {
                case 'M' -> {
                    x = baseX + number();
                    y = baseY + number();
                    startX = x;
                    startY = y;
                    current = new ArrayList<>();
                    current.add(new double[] {x, y});
                    outlines.add(current);
                }
                case 'L' -> {
                    x = baseX + number();
                    y = baseY + number();
                    current = lineTo(outlines, current, x, y);
                }
                case 'H' -> {
                    x = baseX + number();
                    current = lineTo(outlines, current, x, y);
                }
                case 'V' -> {
                    y = baseY + number();
                    current = lineTo(outlines, current, x, y);
                }
                case 'C', 'S' -> {
                    boolean smooth = Character.toUpperCase(command) == 'S';
                    boolean follows = "CcSs".indexOf(previous) >= 0;
                    double firstX = smooth ? (follows ? 2 * x - controlX : x) : baseX + number();
                    double firstY = smooth ? (follows ? 2 * y - controlY : y) : baseY + number();
                    double secondX = baseX + number();
                    double secondY = baseY + number();
                    double endX = baseX + number();
                    double endY = baseY + number();
                    for (int step = 1; step <= STEPS; step++) {
                        double t = (double) step / STEPS;
                        double u = 1 - t;
                        current = lineTo(outlines, current,
                                u * u * u * x + 3 * u * u * t * firstX + 3 * u * t * t * secondX + t * t * t * endX,
                                u * u * u * y + 3 * u * u * t * firstY + 3 * u * t * t * secondY + t * t * t * endY);
                    }
                    controlX = secondX;
                    controlY = secondY;
                    x = endX;
                    y = endY;
                }
                case 'Q', 'T' -> {
                    boolean smooth = Character.toUpperCase(command) == 'T';
                    boolean follows = "QqTt".indexOf(previous) >= 0;
                    double handleX = smooth ? (follows ? 2 * x - controlX : x) : baseX + number();
                    double handleY = smooth ? (follows ? 2 * y - controlY : y) : baseY + number();
                    double endX = baseX + number();
                    double endY = baseY + number();
                    for (int step = 1; step <= STEPS; step++) {
                        double t = (double) step / STEPS;
                        double u = 1 - t;
                        current = lineTo(outlines, current, u * u * x + 2 * u * t * handleX + t * t * endX,
                                u * u * y + 2 * u * t * handleY + t * t * endY);
                    }
                    controlX = handleX;
                    controlY = handleY;
                    x = endX;
                    y = endY;
                }
                case 'A' -> {
                    double radiusX = Math.abs(number());
                    double radiusY = Math.abs(number());
                    double turn = Math.toRadians(number());
                    boolean large = flag();
                    boolean sweep = flag();
                    double endX = baseX + number();
                    double endY = baseY + number();
                    current = arc(outlines, current, x, y, radiusX, radiusY, turn, large, sweep, endX, endY);
                    x = endX;
                    y = endY;
                }
                case 'Z' -> {
                    current = lineTo(outlines, current, startX, startY);
                    x = startX;
                    y = startY;
                }
                default -> throw new IllegalArgumentException("'" + command + "' is no path command: " + data);
            }
            previous = command;
        }
        outlines.removeIf(outline -> outline.size() < 2);
        return outlines;
    }

    private static List<double[]> lineTo(List<List<double[]>> outlines, List<double[]> current, double x, double y) {
        if (current == null) {
            current = new ArrayList<>();
            current.add(new double[] {x, y});
            outlines.add(current);
        }
        current.add(new double[] {x, y});
        return current;
    }

    /** An elliptical arc by its end points, followed in steps (SVG's arc, its centre found as the specification does). */
    private static List<double[]> arc(List<List<double[]>> outlines, List<double[]> current, double fromX, double fromY,
                                      double radiusX, double radiusY, double turn, boolean large, boolean sweep,
                                      double toX, double toY) {
        if (radiusX == 0 || radiusY == 0) {
            return lineTo(outlines, current, toX, toY);
        }
        double cosine = Math.cos(turn);
        double sine = Math.sin(turn);
        double halfX = (fromX - toX) / 2;
        double halfY = (fromY - toY) / 2;
        double primeX = cosine * halfX + sine * halfY;
        double primeY = -sine * halfX + cosine * halfY;
        double scale = primeX * primeX / (radiusX * radiusX) + primeY * primeY / (radiusY * radiusY);
        if (scale > 1) {
            radiusX *= Math.sqrt(scale);
            radiusY *= Math.sqrt(scale);
        }
        double numerator = radiusX * radiusX * radiusY * radiusY - radiusX * radiusX * primeY * primeY
                - radiusY * radiusY * primeX * primeX;
        double denominator = radiusX * radiusX * primeY * primeY + radiusY * radiusY * primeX * primeX;
        double factor = Math.sqrt(Math.max(0, numerator / denominator)) * (large == sweep ? -1 : 1);
        double centrePrimeX = factor * radiusX * primeY / radiusY;
        double centrePrimeY = -factor * radiusY * primeX / radiusX;
        double centreX = cosine * centrePrimeX - sine * centrePrimeY + (fromX + toX) / 2;
        double centreY = sine * centrePrimeX + cosine * centrePrimeY + (fromY + toY) / 2;
        double startAngle = Math.atan2((primeY - centrePrimeY) / radiusY, (primeX - centrePrimeX) / radiusX);
        double endAngle = Math.atan2((-primeY - centrePrimeY) / radiusY, (-primeX - centrePrimeX) / radiusX);
        double span = endAngle - startAngle;
        if (sweep && span < 0) {
            span += 2 * Math.PI;
        } else if (!sweep && span > 0) {
            span -= 2 * Math.PI;
        }
        for (int step = 1; step <= STEPS; step++) {
            double angle = startAngle + span * step / STEPS;
            double ellipseX = radiusX * Math.cos(angle);
            double ellipseY = radiusY * Math.sin(angle);
            current = lineTo(outlines, current, centreX + cosine * ellipseX - sine * ellipseY,
                    centreY + sine * ellipseX + cosine * ellipseY);
        }
        return current;
    }

    private void skipSeparators() {
        while (position < data.length() && (Character.isWhitespace(data.charAt(position)) || data.charAt(position) == ',')) {
            position++;
        }
    }

    /** An arc's flag: one digit, which may stand glued to what follows ({@code 0 01 5 5}). */
    private boolean flag() {
        skipSeparators();
        if (position >= data.length() || (data.charAt(position) != '0' && data.charAt(position) != '1')) {
            throw new IllegalArgumentException("An arc's flag is 0 or 1: " + data);
        }
        return data.charAt(position++) == '1';
    }

    /** A number as path data writes it: a sign, digits, a point, an exponent; {@code -7-9} is two numbers. */
    private double number() {
        skipSeparators();
        int start = position;
        if (position < data.length() && (data.charAt(position) == '-' || data.charAt(position) == '+')) {
            position++;
        }
        boolean point = false;
        while (position < data.length()) {
            char character = data.charAt(position);
            if (Character.isDigit(character)) {
                position++;
            } else if (character == '.' && !point) {
                point = true;
                position++;
            } else if ((character == 'e' || character == 'E') && position + 1 < data.length()
                    && (Character.isDigit(data.charAt(position + 1)) || data.charAt(position + 1) == '-'
                    || data.charAt(position + 1) == '+')) {
                position += 2;
                while (position < data.length() && Character.isDigit(data.charAt(position))) {
                    position++;
                }
                break;
            } else {
                break;
            }
        }
        if (start == position) {
            throw new IllegalArgumentException("A number was expected at " + start + ": " + data);
        }
        return Double.parseDouble(data.substring(start, position));
    }
}
