package org.jmouse.dxf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A drawing in ASCII DXF, as release 12 says it: what every CAD program still reads, and the last release that needs neither
 * object handles nor a table of classes. Entities are added on named layers and the file comes from {@link #write}; it knows
 * nothing of what the lines mean. Coordinates are in the units {@code $MEASUREMENT} names: the drawing says metric.
 */
public final class DxfDrawing {

    /** The characters a layer's name may not hold, as AutoCAD's own rule has it. */
    private static final String REFUSED_IN_A_NAME = "<>/\\\":;?*|=`,";

    /** The colour a layer takes when none is asked: AutoCAD's index 7 prints black on paper. */
    public static final int FOREGROUND = 7;

    private final Map<String, Integer> layers   = new LinkedHashMap<>();
    private final StringBuilder        entities = new StringBuilder();

    /** A point. */
    public record Point(double x, double y) {
    }

    /** Which part of a line of words stands at its point, left to right. */
    public enum Across {
        LEFT(0), CENTRE(1), RIGHT(2);

        private final int code;

        Across(int code) {
            this.code = code;
        }
    }

    /** Which part of a line of words stands at its point, bottom to top. */
    public enum Upright {
        BASELINE(0), BOTTOM(1), MIDDLE(2), TOP(3);

        private final int code;

        Upright(int code) {
            this.code = code;
        }
    }

    /**
     * A layer, declared before it is drawn on, or again to change its colour.
     *
     * @param colour AutoCAD's colour index, from 1 to 255
     */
    public DxfDrawing layer(String name, int colour) {
        requireName(name);
        if (colour < 1 || colour > 255) {
            throw new IllegalArgumentException("A layer's colour is an index from 1 to 255, not " + colour);
        }
        layers.put(name, colour);
        return this;
    }

    /** A straight line. */
    public DxfDrawing line(String layer, Point from, Point to) {
        entity("LINE", layer);
        pair(10, from.x());
        pair(20, from.y());
        pair(11, to.x());
        pair(21, to.y());
        return this;
    }

    /**
     * Straight pieces through points.
     *
     * @param width  how wide the pen is; 0 for the thinnest a device draws
     * @param closed whether the last point runs back to the first
     */
    public DxfDrawing polyline(String layer, List<Point> points, double width, boolean closed) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("A polyline passes through at least two points, not " + points.size());
        }
        entity("POLYLINE", layer);
        pair(66, 1);
        pair(10, 0.0);
        pair(20, 0.0);
        pair(30, 0.0);
        pair(70, closed ? 1 : 0);
        pair(40, width);
        pair(41, width);
        for (Point point : points) {
            entity("VERTEX", layer);
            pair(10, point.x());
            pair(20, point.y());
        }
        entity("SEQEND", layer);
        return this;
    }

    /** A circle. */
    public DxfDrawing circle(String layer, Point centre, double radius) {
        entity("CIRCLE", layer);
        pair(10, centre.x());
        pair(20, centre.y());
        pair(40, radius);
        return this;
    }

    /**
     * An area filled solid: DXF's own fill of three or four corners, which is all release 12 has.
     *
     * @param corners three or four, in order round the area
     */
    public DxfDrawing solid(String layer, List<Point> corners) {
        if (corners.size() != 3 && corners.size() != 4) {
            throw new IllegalArgumentException("A solid has three or four corners, not " + corners.size());
        }
        entity("SOLID", layer);
        // The format's own order crosses over: its third and fourth corners are the far side's, in the same direction.
        Point third = corners.size() == 4 ? corners.get(3) : corners.get(2);
        Point fourth = corners.get(2);
        corner(0, corners.get(0));
        corner(1, corners.get(1));
        corner(2, third);
        corner(3, fourth);
        return this;
    }

    /**
     * Words on one line, starting at a point on their baseline.
     *
     * @param height   how high a capital stands
     * @param rotation degrees anticlockwise
     */
    public DxfDrawing text(String layer, Point at, double height, double rotation, String words) {
        return text(layer, at, height, rotation, Across.LEFT, Upright.BASELINE, words);
    }

    /**
     * Words on one line, standing against a point as asked.
     *
     * @param height   how high a capital stands
     * @param rotation degrees anticlockwise
     */
    public DxfDrawing text(String layer, Point at, double height, double rotation, Across across, Upright upright, String words) {
        entity("TEXT", layer);
        pair(10, at.x());
        pair(20, at.y());
        pair(40, height);
        pair(1, escaped(words));
        pair(50, rotation);
        if (across != Across.LEFT || upright != Upright.BASELINE) {
            pair(72, across.code);
            pair(73, upright.code);
            // Text that is not left on its baseline is placed by its second point.
            pair(11, at.x());
            pair(21, at.y());
        }
        return this;
    }

    /** The file. */
    public String write() {
        StringBuilder file = new StringBuilder();
        section(file, "HEADER", header());
        section(file, "TABLES", tables());
        section(file, "ENTITIES", entities.toString());
        pair(file, 0, "EOF");
        return file.toString();
    }

    private static String header() {
        StringBuilder header = new StringBuilder();
        pair(header, 9, "$ACADVER");
        pair(header, 1, "AC1009");
        pair(header, 9, "$MEASUREMENT");
        pair(header, 70, "1");
        return header.toString();
    }

    private String tables() {
        StringBuilder tables = new StringBuilder();
        pair(tables, 0, "TABLE");
        pair(tables, 2, "LTYPE");
        pair(tables, 70, "1");
        pair(tables, 0, "LTYPE");
        pair(tables, 2, "CONTINUOUS");
        pair(tables, 70, "0");
        pair(tables, 3, "Solid line");
        pair(tables, 72, "65");
        pair(tables, 73, "0");
        pair(tables, 40, number(0));
        pair(tables, 0, "ENDTAB");
        pair(tables, 0, "TABLE");
        pair(tables, 2, "LAYER");
        pair(tables, 70, String.valueOf(layers.size()));
        layers.forEach((name, colour) -> {
            pair(tables, 0, "LAYER");
            pair(tables, 2, name);
            pair(tables, 70, "0");
            pair(tables, 62, String.valueOf(colour));
            pair(tables, 6, "CONTINUOUS");
        });
        pair(tables, 0, "ENDTAB");
        return tables.toString();
    }

    private static void section(StringBuilder file, String name, String body) {
        pair(file, 0, "SECTION");
        pair(file, 2, name);
        file.append(body);
        pair(file, 0, "ENDSEC");
    }

    private void entity(String kind, String layer) {
        if (!layers.containsKey(layer)) {
            layer(layer, FOREGROUND);
        }
        pair(entities, 0, kind);
        pair(entities, 8, layer);
    }

    private void corner(int index, Point point) {
        pair(10 + index, point.x());
        pair(20 + index, point.y());
    }

    private void pair(int code, double value) {
        pair(entities, code, number(value));
    }

    private void pair(int code, int value) {
        pair(entities, code, String.valueOf(value));
    }

    private void pair(int code, String value) {
        pair(entities, code, value);
    }

    private static void pair(StringBuilder into, int code, String value) {
        into.append(String.format(Locale.ROOT, "%3d", code)).append("\r\n").append(value).append("\r\n");
    }

    private static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("A drawing holds finite numbers, not " + value);
        }
        return String.format(Locale.ROOT, "%.6f", value);
    }

    /** Words as an ASCII file can hold them: what is beyond ASCII as DXF's {@code \U+XXXX}, and no line breaks. */
    private static String escaped(String words) {
        StringBuilder written = new StringBuilder();
        words.codePoints().forEach(point -> {
            if (point == '\n' || point == '\r') {
                written.append(' ');
            } else if (point < 0x80) {
                written.appendCodePoint(point);
            } else {
                written.append(String.format(Locale.ROOT, "\\U+%04X", point));
            }
        });
        return written.toString();
    }

    private static void requireName(String name) {
        boolean refused = name == null || name.isBlank() || name.chars().anyMatch(each -> REFUSED_IN_A_NAME.indexOf(each) >= 0);
        if (refused) {
            throw new IllegalArgumentException("A layer's name holds none of " + REFUSED_IN_A_NAME + ", and is not empty: " + name);
        }
    }
}
