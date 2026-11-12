package org.jmouse.odb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What one layer — or a step's profile — holds, written as an ODB++ {@code features} file: the symbols it uses, each
 * once in a table, then its features in the order given, the later over the earlier. Coordinates are millimetres with
 * Y upwards; a feature is dark ({@code P}) unless it is said to take away ({@code N}).
 */
public final class OdbFeatures {

    /** A point of a layer, in millimetres, Y upwards. */
    public record Point(double x, double y) {
    }

    /** One closed contour of a surface: its points in order, the first not repeated at the end. */
    public record Contour(List<Point> points) {
        public Contour {
            points = List.copyOf(points);
            if (points.size() < 3) {
                throw new IllegalArgumentException("A contour closes round three points at least.");
            }
        }
    }

    private final Map<String, Integer> symbols  = new LinkedHashMap<>();
    private final List<String>         features = new ArrayList<>();
    private       int                  count;

    /** A line from a point to a point, drawn with a symbol — a track, an outline's stroke. */
    public OdbFeatures line(Point from, Point to, OdbSymbol symbol, boolean dark) {
        count++;
        features.add("L " + at(from) + " " + at(to) + " " + symbolOf(symbol) + " " + polarity(dark) + " 0");
        return this;
    }

    /** A symbol flashed at a point, turned by {@code rotation} degrees clockwise — a pad, a via, a hole. */
    public OdbFeatures pad(Point at, OdbSymbol symbol, double rotation, boolean dark) {
        count++;
        String orientation = rotation == 0 ? "0" : "8 " + OdbNumbers.millimetres(rotation);
        features.add("P " + at(at) + " " + symbolOf(symbol) + " " + polarity(dark) + " 0 " + orientation);
        return this;
    }

    /** An arc from a point to a point about a centre, drawn with a symbol, clockwise as the coordinates run or not. */
    public OdbFeatures arc(Point from, Point to, Point centre, OdbSymbol symbol, boolean clockwise, boolean dark) {
        count++;
        features.add("A " + at(from) + " " + at(to) + " " + at(centre) + " " + symbolOf(symbol) + " " + polarity(dark) + " 0 "
                + (clockwise ? "Y" : "N"));
        return this;
    }

    /** A filled area: its outline an island, each hole cut out of it. */
    public OdbFeatures surface(Contour outline, List<Contour> holes, boolean dark) {
        count++;
        features.add("S " + polarity(dark) + " 0");
        contour(outline, "I");
        holes.forEach(hole -> contour(hole, "H"));
        features.add("SE");
        return this;
    }

    /** How many features are drawn: the number the next one will be, as {@code eda/data} links to it. */
    public int count() {
        return count;
    }

    /** Whether nothing is drawn. */
    public boolean isEmpty() {
        return features.isEmpty();
    }

    /** The file. */
    public String text() {
        StringBuilder text = new StringBuilder("UNITS=MM\n#\n#Feature symbol names\n#\n");
        symbols.forEach((name, index) -> text.append('$').append(index).append(' ').append(name).append('\n'));
        text.append("#\n#Layer features\n#\n");
        features.forEach(feature -> text.append(feature).append('\n'));
        return text.toString();
    }

    private void contour(Contour contour, String kind) {
        List<Point> points = contour.points();
        features.add("OB " + at(points.getFirst()) + " " + kind);
        for (Point point : points.subList(1, points.size())) {
            features.add("OS " + at(point));
        }
        features.add("OS " + at(points.getFirst()));
        features.add("OE");
    }

    private int symbolOf(OdbSymbol symbol) {
        return symbols.computeIfAbsent(symbol.name(), ignored -> symbols.size());
    }

    private static String at(Point point) {
        return OdbNumbers.millimetres(point.x()) + " " + OdbNumbers.millimetres(point.y());
    }

    private static String polarity(boolean dark) {
        return dark ? "P" : "N";
    }
}
