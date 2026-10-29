package org.jmouse.cad.kicad;

import org.jmouse.cad.board.BoardModule;
import org.jmouse.cad.board.FootprintLines;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.document.MetaLineNode;
import org.jmouse.cad.document.MetaNode;
import org.jmouse.el.grammar.Nodes;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.footprint.KiCadPad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A KiCad footprint as a footprint document, built as its tree: its pads as they are, and the lines it draws as path
 * data — one outline for each layer, stroke and fill. Both are in millimetres with Y downwards, so nothing is scaled or
 * turned.
 *
 * <p>What a layer is for decides where it goes ({@link KiCadLibraries#layerOf}): KiCad's silkscreen, fabrication and
 * courtyard of either side, and the copper a footprint draws besides its pads. Its edge cuts, user layers and texts are
 * not a part's.</p>
 */
public final class KiCadFootprintDrawing {

    /** The summary a catalogue object may carry, in characters. */
    private static final int SUMMARY_LENGTH = 480;

    /** A length to a thousandth of a millimetre: a micrometre is below what a board is made to. */
    private static final int PLACES = 3;

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

    /**
     * The footprint's document: what it is called, what KiCad says of it, its pads and its outlines, and whose it is — one
     * of KiCad's installed libraries is the KiCad Libraries Team's under their licence; one of the project's own claims
     * neither a holder nor terms, which are the project's.
     */
    public static DocumentNode documentOf(String libraryId, KiCadFootprint footprint, KiCadFootprintLibraries.Origin origin,
                                          KiCadLibraries libraries) {
        String said = footprint.description().isBlank() ? footprint.name() : footprint.description().strip();
        boolean installed = origin == KiCadFootprintLibraries.Origin.INSTALLED;
        DocumentNode document = new DocumentNode(BoardModule.FOOTPRINT, footprint.name(), slugOf(libraryId), true, null, 1);
        MetaNode meta = new MetaNode();
        meta.addExpression(MetaLineNode.of(MetaNode.SUMMARY, Nodes.text(shortened(said + " — imported from KiCad"))));
        meta.addExpression(MetaLineNode.of(MetaNode.CATEGORY, Nodes.word(libraries.category())));
        meta.addExpression(MetaLineNode.of(MetaNode.TAGS, Nodes.words(libraries.footprintTags())));
        meta.addExpression(MetaLineNode.of(MetaNode.SOURCE, Nodes.text(KiCadSources.footprint(libraryId, installed))));
        if (installed) {
            meta.addExpression(MetaLineNode.of(MetaNode.AUTHORS, Nodes.text(libraries.holder())));
            meta.addExpression(MetaLineNode.of(MetaNode.LICENSE, Nodes.text(libraries.license())));
        }
        document.addExpression(meta);
        footprint.pads().forEach(pad -> document.addExpression(FootprintLines.pad(pad(pad), List.of())));
        outlinesOf(footprint.graphics(), libraries)
                .forEach(outline -> document.addExpression(FootprintLines.outline(outline, List.of())));
        return document;
    }

    private static FootprintLines.Pad pad(KiCadPad pad) {
        String type = switch (pad.type()) {
            case THROUGH_HOLE -> "through-hole";
            case SURFACE_MOUNT -> "surface";
            case CONNECTOR -> "connector";
            case UNPLATED_HOLE -> "unplated-hole";
        };
        String shape = switch (pad.shape()) {
            case CIRCLE -> "circle";
            case RECTANGLE -> "rectangle";
            case OVAL -> "oval";
            case TRAPEZOID -> "trapezoid";
            case ROUNDED_RECTANGLE -> "rounded-rectangle";
            case CUSTOM -> "custom";
        };
        RoundedPath round = new RoundedPath(PLACES);
        double[] drill = pad.drill().orElse(new double[] {0, 0});
        return new FootprintLines.Pad(pad.number(), type, shape, round.rounded(pad.at().x()), round.rounded(pad.at().y()),
                round.rounded(pad.angle()), round.rounded(pad.width()), round.rounded(pad.height()), round.rounded(drill[0]),
                round.rounded(drill[1]), round.rounded(pad.roundness()), Optional.empty());
    }

    /** One outline for each layer, stroke and fill, its shapes in the order KiCad draws them. */
    private static List<FootprintLines.Outline> outlinesOf(List<KiCadFootprintGraphic> graphics, KiCadLibraries libraries) {
        record Stroke(String layer, double width, boolean filled) {
        }
        Map<Stroke, RoundedPath> paths = new LinkedHashMap<>();
        for (KiCadFootprintGraphic graphic : graphics) {
            Optional<String> layer = libraries.layerOf(graphic.layer());
            if (layer.isEmpty()) {
                continue;
            }
            Stroke stroke = new Stroke(layer.get(), new RoundedPath(PLACES).rounded(graphic.width()), graphic.filled());
            draw(graphic, paths.computeIfAbsent(stroke, ignored -> new RoundedPath(PLACES)));
        }
        List<FootprintLines.Outline> outlines = new ArrayList<>();
        paths.forEach((stroke, path) -> {
            if (!path.isEmpty()) {
                outlines.add(new FootprintLines.Outline(stroke.layer(), stroke.width(), stroke.filled(), path.build()));
            }
        });
        return outlines;
    }

    private static void draw(KiCadFootprintGraphic graphic, RoundedPath path) {
        switch (graphic) {
            case KiCadFootprintGraphic.Line line -> path.moveTo(line.start().x(), line.start().y())
                    .lineTo(line.end().x(), line.end().y());
            case KiCadFootprintGraphic.Rectangle rectangle -> {
                KiCadPoint start = rectangle.start();
                KiCadPoint end = rectangle.end();
                path.moveTo(start.x(), start.y()).lineTo(end.x(), start.y()).lineTo(end.x(), end.y())
                        .lineTo(start.x(), end.y()).close();
            }
            case KiCadFootprintGraphic.Circle circle -> path.circle(circle.center().x(), circle.center().y(), circle.radius());
            case KiCadFootprintGraphic.Arc arc -> path.arcThrough(arc.start().x(), arc.start().y(), arc.middle().x(),
                    arc.middle().y(), arc.end().x(), arc.end().y());
            case KiCadFootprintGraphic.Polygon polygon -> {
                if (!polygon.points().isEmpty()) {
                    path.moveTo(polygon.points().getFirst().x(), polygon.points().getFirst().y());
                    polygon.points().subList(1, polygon.points().size()).forEach(point -> path.lineTo(point.x(), point.y()));
                    path.close();
                }
            }
        }
    }

    private static String shortened(String text) {
        if (text.length() <= SUMMARY_LENGTH) {
            return text;
        }
        int space = text.lastIndexOf(' ', SUMMARY_LENGTH);
        return text.substring(0, space > 0 ? space : SUMMARY_LENGTH) + "…";
    }
}
