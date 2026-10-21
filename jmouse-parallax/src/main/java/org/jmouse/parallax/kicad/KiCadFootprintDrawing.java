package org.jmouse.parallax.kicad;

import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.FootprintOutline;
import org.jmouse.parallax.script.Block.FootprintPad;
import org.jmouse.parallax.script.ExperienceDocument;
import org.jmouse.parallax.script.Kind;
import org.jmouse.parallax.script.Meta;
import org.jmouse.parallax.script.writer.DocumentWriter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A KiCad footprint as a jMX footprint: its pads as they are, and the lines it draws as path data — one outline for each
 * layer, stroke and fill —, built as the reader would have built it and written by {@link DocumentWriter}. Both are in
 * millimetres with Y downwards, so nothing is scaled or turned.
 *
 * <p>What a layer is for decides where it goes: KiCad's silkscreen, fabrication and courtyard of either side, and the
 * copper a footprint draws besides its pads. Its edge cuts, user layers and texts are not a part's.</p>
 */
public final class KiCadFootprintDrawing {

    /** KiCad's layers by what they are for, front and back alike. */
    private static final Map<String, String> LAYERS = Map.of(
            "F.SilkS", "silkscreen", "B.SilkS", "silkscreen",
            "F.Fab", "fabrication", "B.Fab", "fabrication",
            "F.CrtYd", "courtyard", "B.CrtYd", "courtyard",
            "F.Cu", "copper", "B.Cu", "copper");

    /** The summary a catalogue object may carry, in characters. */
    private static final int SUMMARY_LENGTH = 480;

    private KiCadFootprintDrawing() {
    }

    /**
     * The slug of a footprint by its library id, as the import's symbols are named:
     * {@code Package_TO_SOT_THT:TO-220-3_Vertical} is {@code kicad-package-to-sot-tht-to-220-3-vertical-footprint}.
     */
    public static String slugOf(String libraryId) {
        return "kicad-" + libraryId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "")
                + "-footprint";
    }

    /** The library a footprint comes from: {@code Package_TO_SOT_THT} of {@code Package_TO_SOT_THT:TO-220-3_Vertical}. */
    public static String libraryOf(String libraryId) {
        int colon = libraryId.indexOf(':');
        return colon < 0 ? "" : libraryId.substring(0, colon);
    }

    /** The footprint's document: what it is called, what KiCad says of it, its pads and its outlines. */
    public static ExperienceDocument documentOf(String libraryId, KiCadFootprint footprint) {
        String said = footprint.description().isBlank() ? footprint.name() : footprint.description().strip();
        String summary = shortened(said + " — imported from KiCad");
        List<String> tags = new ArrayList<>(List.of("electronics", "kicad", "footprint"));
        Meta meta = new Meta(summary, null, "circuits", tags, "KiCad footprint " + libraryId, List.of(), null, List.of(),
                Map.of(), Map.of());
        Block.Footprint drawing = new Block.Footprint(footprint.pads().stream().map(KiCadFootprintDrawing::pad).toList(),
                outlinesOf(footprint.graphics()));
        return new ExperienceDocument(Kind.FOOTPRINT, footprint.name(), slugOf(libraryId), Optional.empty(), meta,
                Optional.empty(), List.of(), List.of(), List.of(drawing), 0);
    }

    private static FootprintPad pad(KiCadPad pad) {
        Block.PadType type = switch (pad.type()) {
            case THROUGH_HOLE -> Block.PadType.THROUGH_HOLE;
            case SURFACE_MOUNT -> Block.PadType.SURFACE;
            case CONNECTOR -> Block.PadType.CONNECTOR;
            case UNPLATED_HOLE -> Block.PadType.UNPLATED_HOLE;
        };
        Block.PadShape shape = switch (pad.shape()) {
            case CIRCLE -> Block.PadShape.CIRCLE;
            case RECTANGLE -> Block.PadShape.RECTANGLE;
            case OVAL -> Block.PadShape.OVAL;
            case TRAPEZOID -> Block.PadShape.TRAPEZOID;
            case ROUNDED_RECTANGLE -> Block.PadShape.ROUNDED_RECTANGLE;
            case CUSTOM -> Block.PadShape.CUSTOM;
        };
        double[] drill = pad.drill().orElse(new double[] {0, 0});
        return new FootprintPad(pad.number(), type, shape, rounded(pad.at().x()), rounded(pad.at().y()),
                rounded(pad.angle()), rounded(pad.width()), rounded(pad.height()), rounded(drill[0]), rounded(drill[1]),
                rounded(pad.roundness()));
    }

    /** One outline for each layer, stroke and fill, its shapes in the order KiCad draws them. */
    private static List<FootprintOutline> outlinesOf(List<KiCadFootprintGraphic> graphics) {
        record Stroke(String layer, double width, boolean filled) {
        }
        Map<Stroke, StringBuilder> paths = new LinkedHashMap<>();
        for (KiCadFootprintGraphic graphic : graphics) {
            String layer = LAYERS.get(graphic.layer());
            if (layer == null) {
                continue;
            }
            paths.computeIfAbsent(new Stroke(layer, rounded(graphic.width()), graphic.filled()),
                    ignored -> new StringBuilder()).append(pathOf(graphic));
        }
        List<FootprintOutline> outlines = new ArrayList<>();
        paths.forEach((stroke, path) -> outlines.add(new FootprintOutline(stroke.layer(), stroke.width(), stroke.filled(),
                path.toString())));
        return outlines;
    }

    private static String pathOf(KiCadFootprintGraphic graphic) {
        return switch (graphic) {
            case KiCadFootprintGraphic.Line line -> "M" + pair(line.start()) + "L" + pair(line.end());
            case KiCadFootprintGraphic.Rectangle rectangle -> "M" + pair(rectangle.start())
                    + "L" + number(rectangle.end().x()) + " " + number(rectangle.start().y())
                    + "L" + pair(rectangle.end())
                    + "L" + number(rectangle.start().x()) + " " + number(rectangle.end().y()) + "Z";
            case KiCadFootprintGraphic.Circle circle -> circlePath(circle.center(), circle.radius());
            case KiCadFootprintGraphic.Arc arc -> arcPath(arc.start(), arc.middle(), arc.end());
            case KiCadFootprintGraphic.Polygon polygon -> polygonPath(polygon.points());
        };
    }

    private static String circlePath(KiCadPoint center, double radius) {
        String r = number(radius);
        return "M" + number(center.x() - radius) + " " + number(center.y()) + "A" + r + " " + r + " 0 1 0 "
                + number(center.x() + radius) + " " + number(center.y()) + "A" + r + " " + r + " 0 1 0 "
                + number(center.x() - radius) + " " + number(center.y()) + "Z";
    }

    /** The circle through the three, from start through middle to end; a line when they lie on one. */
    private static String arcPath(KiCadPoint start, KiCadPoint middle, KiCadPoint end) {
        double determinant = 2 * (start.x() * (middle.y() - end.y()) + middle.x() * (end.y() - start.y())
                + end.x() * (start.y() - middle.y()));
        if (Math.abs(determinant) < 1e-9) {
            return "M" + pair(start) + "L" + pair(end);
        }
        double startSquare = start.x() * start.x() + start.y() * start.y();
        double middleSquare = middle.x() * middle.x() + middle.y() * middle.y();
        double endSquare = end.x() * end.x() + end.y() * end.y();
        double centerX = (startSquare * (middle.y() - end.y()) + middleSquare * (end.y() - start.y())
                + endSquare * (start.y() - middle.y())) / determinant;
        double centerY = (startSquare * (end.x() - middle.x()) + middleSquare * (start.x() - end.x())
                + endSquare * (middle.x() - start.x())) / determinant;
        double radius = Math.hypot(start.x() - centerX, start.y() - centerY);
        double turn = (middle.x() - start.x()) * (end.y() - start.y()) - (middle.y() - start.y()) * (end.x() - start.x());
        // The middle on the side a positive turn puts it: the angle grows from start to end, SVG's sweep flag 1.
        boolean sweep = turn > 0;
        double from = Math.atan2(start.y() - centerY, start.x() - centerX);
        double to = Math.atan2(end.y() - centerY, end.x() - centerX);
        double span = sweep ? to - from : from - to;
        span = ((span % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        String flags = (span > Math.PI ? "1 " : "0 ") + (sweep ? "1 " : "0 ");
        return "M" + pair(start) + "A" + number(radius) + " " + number(radius) + " 0 " + flags + pair(end);
    }

    private static String polygonPath(List<KiCadPoint> points) {
        if (points.isEmpty()) {
            return "";
        }
        StringBuilder path = new StringBuilder("M").append(pair(points.getFirst()));
        points.subList(1, points.size()).forEach(point -> path.append("L").append(pair(point)));
        return path.append("Z").toString();
    }

    private static String shortened(String text) {
        if (text.length() <= SUMMARY_LENGTH) {
            return text;
        }
        int space = text.lastIndexOf(' ', SUMMARY_LENGTH);
        return text.substring(0, space > 0 ? space : SUMMARY_LENGTH) + "…";
    }

    private static String pair(KiCadPoint point) {
        return number(point.x()) + " " + number(point.y());
    }

    /** A length to a thousandth of a millimetre: a micrometre is below what a board is made to. */
    private static double rounded(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_EVEN).doubleValue() + 0.0;
    }

    /** A coordinate of path data, as short as it reads back. */
    private static String number(double value) {
        return BigDecimal.valueOf(rounded(value)).stripTrailingZeros().toPlainString();
    }
}
