package org.jmouse.strokefont;

import org.jmouse.strokefont.node.FontoBeneLine;

import java.util.ArrayList;
import java.util.List;

/**
 * Text laid out in a stroke font, as the centre lines of its strokes in the drawing's own units, Y down: glyph after
 * glyph, each placed by its trailing space and the font's letter spacing (FontoBene 4.1.1); line under line by the
 * font's line spacing; an arc of a stroke in straight pieces. How thick a stroke is drawn is the caller's.
 *
 * @param strokes every stroke, the text's anchor point at the origin
 * @param width   the widest line, in the drawing's units
 * @param height  from the first cap's top to the last baseline
 */
public record StrokeText(List<List<Point>> strokes, double width, double height) {

    /** A point of a stroke in the drawing's units. */
    public record Point(double x, double y) {
    }

    /** Where the point the text is placed at stands along it. */
    public enum Anchor {
        START, MIDDLE, END
    }

    /** Where the point stands across it: the first baseline, the middle of the caps, their top, or the last baseline. */
    public enum Baseline {
        ALPHABETIC, MIDDLE, TOP, BOTTOM
    }

    /** What a character the font has no glyph for is drawn as: the replacement character, else a question mark. */
    private static final int[] REPLACEMENTS = {0xFFFD, 0x3F};

    /** A glyph and where its origin stands along its line, in the font's units. */
    private record Placed(StrokeFont.Glyph glyph, double at) {
    }

    private record Line(List<Placed> placed, double width) {
    }

    public StrokeText {
        strokes = strokes.stream().map(List::copyOf).toList();
    }

    /**
     * @param height   how high a capital stands, in the drawing's units
     * @param arcSteps how many straight pieces an arc of a stroke is drawn with
     */
    public static StrokeText lay(StrokeFont font, String text, double height, Anchor anchor, Baseline baseline, int arcSteps) {
        double scale = height / StrokeFont.CAP_HEIGHT;
        List<Line> lines = text.lines().map(line -> lineOf(font, line)).toList();
        double lineStep = font.lineSpacing() * scale;
        double width = lines.stream().mapToDouble(Line::width).max().orElse(0) * scale;
        int rows = Math.max(lines.size(), 1);
        double firstBaseline = switch (baseline) {
            case ALPHABETIC -> 0;
            case TOP -> height;
            case MIDDLE -> height / 2 - (rows - 1) * lineStep / 2;
            case BOTTOM -> -(rows - 1) * lineStep;
        };
        List<List<Point>> strokes = new ArrayList<>();
        for (int row = 0; row < lines.size(); row++) {
            Line line = lines.get(row);
            double shift = switch (anchor) {
                case START -> 0;
                case MIDDLE -> -line.width() * scale / 2;
                case END -> -line.width() * scale;
            };
            double baselineY = firstBaseline + row * lineStep;
            for (Placed placed : line.placed()) {
                for (List<FontoBeneLine.Vertex> polyline : placed.glyph().polylines()) {
                    List<Point> stroke = new ArrayList<>();
                    for (double[] point : traced(polyline, arcSteps)) {
                        stroke.add(new Point(shift + (placed.at() + point[0]) * scale, baselineY - point[1] * scale));
                    }
                    strokes.add(stroke);
                }
            }
        }
        return new StrokeText(strokes, width, height + (rows - 1) * lineStep);
    }

    private static Line lineOf(StrokeFont font, String text) {
        List<Placed> placed = new ArrayList<>();
        double cursor = 0;
        double width = 0;
        for (int codepoint : text.codePoints().toArray()) {
            StrokeFont.Glyph glyph = glyphOf(font, codepoint);
            if (glyph == null) {
                continue;
            }
            placed.add(new Placed(glyph, cursor));
            width = cursor + glyph.right() + glyph.trailing();
            cursor = width + font.letterSpacing();
        }
        return new Line(placed, width);
    }

    private static StrokeFont.Glyph glyphOf(StrokeFont font, int codepoint) {
        StrokeFont.Glyph glyph = font.glyphs().get(codepoint);
        for (int index = 0; glyph == null && index < REPLACEMENTS.length; index++) {
            glyph = font.glyphs().get(REPLACEMENTS[index]);
        }
        return glyph;
    }

    /**
     * A polyline's points in the font's units: straight to the next vertex, or along an arc of central angle bulge × 20°,
     * counter-clockwise positive as the font's Y runs up.
     */
    private static List<double[]> traced(List<FontoBeneLine.Vertex> polyline, int arcSteps) {
        List<double[]> points = new ArrayList<>();
        for (int index = 0; index < polyline.size(); index++) {
            FontoBeneLine.Vertex vertex = polyline.get(index);
            if (index > 0 && polyline.get(index - 1).bulge() != 0) {
                FontoBeneLine.Vertex from = polyline.get(index - 1);
                double angle = from.bulge() * Math.PI / 9;
                double chordX = vertex.x() - from.x();
                double chordY = vertex.y() - from.y();
                double chord = Math.hypot(chordX, chordY);
                // The centre stands off the chord's middle, to its left for a counter-clockwise arc.
                double offset = chord / 2 / Math.tan(angle / 2);
                double centreX = from.x() + chordX / 2 - chordY / chord * offset;
                double centreY = from.y() + chordY / 2 + chordX / chord * offset;
                double start = Math.atan2(from.y() - centreY, from.x() - centreX);
                double radius = Math.hypot(from.x() - centreX, from.y() - centreY);
                for (int step = 1; step < arcSteps; step++) {
                    double at = start + angle * step / arcSteps;
                    points.add(new double[] {centreX + radius * Math.cos(at), centreY + radius * Math.sin(at)});
                }
            }
            points.add(new double[] {vertex.x(), vertex.y()});
        }
        return points;
    }
}
