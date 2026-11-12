package org.jmouse.dxf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What a DXF file draws, read as closed outlines in millimetres: its lines joined end to end, its arcs and bulged polylines
 * walked in short steps, its circles whole. It reads the plain geometry of the {@code ENTITIES} section, which is what a mechanical
 * program exports for a board's edge; anything else in the file is counted, not guessed at.
 */
public final class DxfOutline {

    /** How far apart two ends may be and still be one point, in millimetres. */
    private static final double SAME_POINT = 0.01;

    /** How many degrees of an arc one straight step takes. */
    private static final double STEP_DEGREES = 5;

    /** The value of {@code $INSUNITS} for inches: a drawing in it is multiplied up. */
    private static final int UNITS_INCHES = 1;
    private static final int UNITS_FEET = 2;
    private static final int UNITS_MILLIMETRES = 4;
    private static final int UNITS_CENTIMETRES = 5;
    private static final int UNITS_METRES = 6;

    private static final double MILLIMETRES_PER_INCH = 25.4;

    /** A point, in millimetres, Y up as the file has it. */
    public record Point(double x, double y) {
        double distanceTo(Point other) {
            return Math.hypot(x - other.x, y - other.y);
        }
    }

    /**
     * What was read.
     *
     * @param closed   the closed outlines, each without its last point repeated
     * @param open     how many runs of lines never came back to where they began: left out, and said
     * @param skipped  how many entities of a kind this does not read
     * @param unitless whether the file says no unit, and millimetres were taken
     */
    public record Read(List<List<Point>> closed, int open, int skipped, boolean unitless) {
    }

    private DxfOutline() {
    }

    /** A pair of a DXF file: its group code and its value. */
    private record Pair(int code, String value) {
    }

    public static Read read(String text) {
        List<Pair> pairs = pairsOf(text);
        double factor = 1;
        boolean unitless = true;
        for (int index = 0; index + 1 < pairs.size(); index++) {
            if (pairs.get(index).code() == 9 && pairs.get(index).value().equals("$INSUNITS")) {
                int units = (int) number(pairs.get(index + 1).value());
                unitless = units == 0;
                factor = factorOf(units);
            }
        }
        List<List<Point>> runs = new ArrayList<>();
        int skipped = 0;
        int index = sectionStart(pairs);
        if (index < 0) {
            throw new IllegalArgumentException("It is no DXF drawing: it has no ENTITIES section.");
        }
        while (index < pairs.size()) {
            Pair pair = pairs.get(index);
            if (pair.code() == 0 && pair.value().equals("ENDSEC")) {
                break;
            }
            if (pair.code() != 0) {
                index++;
                continue;
            }
            int end = index + 1;
            while (end < pairs.size() && pairs.get(end).code() != 0) {
                end++;
            }
            List<Pair> fields = pairs.subList(index + 1, end);
            switch (pair.value()) {
                case "LINE" -> runs.add(List.of(new Point(value(fields, 10), value(fields, 20)), new Point(value(fields, 11), value(fields, 21))));
                case "ARC" -> runs.add(arc(value(fields, 10), value(fields, 20), value(fields, 40), value(fields, 50), value(fields, 51)));
                case "CIRCLE" -> runs.add(arc(value(fields, 10), value(fields, 20), value(fields, 40), 0, 360));
                case "LWPOLYLINE" -> runs.add(polyline(fields));
                case "VERTEX", "SEQEND", "ENDSEC" -> {
                }
                default -> skipped++;
            }
            index = end;
        }
        double scale = factor;
        List<List<Point>> scaled = runs.stream().map(run -> run.stream().map(point -> new Point(point.x() * scale, point.y() * scale)).toList()).toList();
        return joined(scaled, skipped, unitless);
    }

    private static List<Pair> pairsOf(String text) {
        String[] lines = text.split("\\r?\\n");
        List<Pair> pairs = new ArrayList<>();
        for (int index = 0; index + 1 < lines.length; index += 2) {
            try {
                pairs.add(new Pair(Integer.parseInt(lines[index].trim()), lines[index + 1].trim()));
            } catch (NumberFormatException notAPair) {
                throw new IllegalArgumentException("It is no text DXF: line " + (index + 1) + " holds no group code.", notAPair);
            }
        }
        return pairs;
    }

    private static int sectionStart(List<Pair> pairs) {
        for (int index = 0; index + 1 < pairs.size(); index++) {
            if (pairs.get(index).code() == 2 && pairs.get(index).value().toUpperCase(Locale.ROOT).equals("ENTITIES")) {
                return index + 1;
            }
        }
        return -1;
    }

    private static double factorOf(int units) {
        return switch (units) {
            case UNITS_INCHES -> MILLIMETRES_PER_INCH;
            case UNITS_FEET -> 12 * MILLIMETRES_PER_INCH;
            case UNITS_MILLIMETRES -> 1;
            case UNITS_CENTIMETRES -> 10;
            case UNITS_METRES -> 1000;
            default -> 1;
        };
    }

    private static double number(String value) {
        return Double.parseDouble(value);
    }

    private static double value(List<Pair> fields, int code) {
        return fields.stream().filter(field -> field.code() == code).findFirst().map(field -> number(field.value())).orElse(0.0);
    }

    private static List<Point> arc(double x, double y, double radius, double from, double to) {
        double sweep = to > from ? to - from : to + 360 - from;
        int steps = Math.max(1, (int) Math.ceil(sweep / STEP_DEGREES));
        List<Point> points = new ArrayList<>();
        for (int step = 0; step <= steps; step++) {
            double angle = Math.toRadians(from + sweep * step / steps);
            points.add(new Point(x + radius * Math.cos(angle), y + radius * Math.sin(angle)));
        }
        return points;
    }

    /** A polyline's corners, each bulge walked as the arc it stands for; closed by its flag. */
    private static List<Point> polyline(List<Pair> fields) {
        List<double[]> corners = new ArrayList<>();
        boolean closed = false;
        for (Pair field : fields) {
            switch (field.code()) {
                case 70 -> closed = ((int) number(field.value()) & 1) == 1;
                case 10 -> corners.add(new double[] {number(field.value()), 0, 0});
                case 20 -> corners.getLast()[1] = number(field.value());
                case 42 -> corners.getLast()[2] = number(field.value());
                default -> {
                }
            }
        }
        List<Point> points = new ArrayList<>();
        int segments = closed ? corners.size() : corners.size() - 1;
        for (int index = 0; index < segments; index++) {
            double[] from = corners.get(index);
            double[] to = corners.get((index + 1) % corners.size());
            points.add(new Point(from[0], from[1]));
            if (from[2] != 0) {
                bulged(points, from, to);
            }
        }
        if (!corners.isEmpty()) {
            double[] last = corners.get(closed ? 0 : corners.size() - 1);
            points.add(new Point(last[0], last[1]));
        }
        return points;
    }

    /** The points inside a bulged stretch: a bulge is the tangent of a quarter of the arc's angle, its sign its way round. */
    private static void bulged(List<Point> points, double[] from, double[] to) {
        double angle = 4 * Math.atan(from[2]);
        double chord = Math.hypot(to[0] - from[0], to[1] - from[1]);
        double radius = chord / (2 * Math.sin(Math.abs(angle) / 2));
        double middle = Math.atan2(to[1] - from[1], to[0] - from[0]) + Math.signum(angle) * (Math.PI / 2 - Math.abs(angle) / 2);
        double centreX = from[0] + radius * Math.cos(middle);
        double centreY = from[1] + radius * Math.sin(middle);
        double start = Math.atan2(from[1] - centreY, from[0] - centreX);
        int steps = Math.max(1, (int) Math.ceil(Math.toDegrees(Math.abs(angle)) / STEP_DEGREES));
        for (int step = 1; step < steps; step++) {
            double at = start + angle * step / steps;
            points.add(new Point(centreX + radius * Math.cos(at), centreY + radius * Math.sin(at)));
        }
    }

    /** Runs joined end to end, either way round, until they close. */
    private static Read joined(List<List<Point>> runs, int skipped, boolean unitless) {
        List<List<Point>> left = new ArrayList<>(runs.stream().filter(run -> run.size() > 1).toList());
        List<List<Point>> closed = new ArrayList<>();
        int open = 0;
        while (!left.isEmpty()) {
            List<Point> outline = new ArrayList<>(left.removeFirst());
            boolean grew = true;
            while (grew && outline.getFirst().distanceTo(outline.getLast()) > SAME_POINT) {
                grew = false;
                for (int index = 0; index < left.size(); index++) {
                    List<Point> run = left.get(index);
                    Point end = outline.getLast();
                    if (run.getFirst().distanceTo(end) <= SAME_POINT) {
                        outline.addAll(run.subList(1, run.size()));
                    } else if (run.getLast().distanceTo(end) <= SAME_POINT) {
                        outline.addAll(run.reversed().subList(1, run.size()));
                    } else {
                        continue;
                    }
                    left.remove(index);
                    grew = true;
                    break;
                }
            }
            if (outline.size() > 2 && outline.getFirst().distanceTo(outline.getLast()) <= SAME_POINT) {
                closed.add(List.copyOf(outline.subList(0, outline.size() - 1)));
            } else {
                open++;
            }
        }
        return new Read(closed, open, skipped, unitless);
    }
}
