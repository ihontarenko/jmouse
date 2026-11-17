package org.jmouse.cad.kicad;

import org.jmouse.kicad.pcb.KiCadPcb;
import org.jmouse.cad.exchange.imported.ImportedFields;
import org.jmouse.cad.document.TechnicalClass;
import org.jmouse.cad.document.Provenance;
import org.jmouse.cad.exchange.imported.ImportedNames;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.exchange.imported.FootprintOutlines;
import org.jmouse.cad.exchange.imported.ImportedFootprint;
import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.kicad.KiCadProperty;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.kicad.KiCadPoint;
import org.jmouse.kicad.pcb.KiCadSide;
import org.jmouse.kicad.footprint.KiCadFootprint;
import org.jmouse.kicad.footprint.KiCadFootprintGraphic;
import org.jmouse.kicad.footprint.KiCadFootprintLibraries;
import org.jmouse.kicad.footprint.KiCadModel;
import org.jmouse.kicad.KiCadLayer;
import org.jmouse.kicad.footprint.KiCadPad;
import org.jmouse.kicad.footprint.KiCadPadShape;
import org.jmouse.kicad.footprint.KiCadPadType;
import org.jmouse.svg.path.node.PathData;

import java.util.ArrayList;
import java.util.List;
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


    /** A length to a thousandth of a millimetre, as every converted footprint keeps it. */
    private static final int PLACES = FootprintOutlines.PLACES;

    /** How many straight pieces a window's rounded corner is written in. */
    private static final int WINDOW_CORNER_STEPS = 8;

    private KiCadFootprintDrawing() {
    }

    /**
     * The slug of a footprint by its library id, as the import's symbols are named:
     * {@code Package_TO_SOT_THT:TO-220-3_Vertical} is {@code kicad-package-to-sot-tht-to-220-3-vertical-footprint}.
     */
    public static String slugOf(String libraryId) {
        return ImportedNames.slug(KiCadSymbolDrawing.KICAD, libraryId, "footprint");
    }

    /**
     * The slug of a 3D model by its library path, as its footprint's is made:
     * {@code Package_SO.3dshapes/SOIC-8_3.9x4.9mm_P1.27mm} is {@code kicad-package-so-soic-8-3-9x4-9mm-p1-27mm-model}.
     */
    public static String modelSlugOf(String libraryPath) {
        return ImportedNames.slug(KiCadSymbolDrawing.KICAD, libraryPath.replace(KiCadSources.MODEL_LIBRARY, "-"), "model");
    }

    /**
     * The code a footprint converted from KiCad is named by, and every reference to it names it by: its library id in
     * capitals — {@code Package_SO:SOIC-8_3.9x4.9mm_P1.27mm} is {@code PACKAGE-SO:SOIC-8-3.9X4.9MM-P1.27MM:F}.
     */
    public static String codeOf(String libraryId) {
        int colon = libraryId.indexOf(':');
        return ImportedNames.code(TechnicalClass.FOOTPRINT, colon < 0 ? KiCadSymbolDrawing.KICAD : libraryId.substring(0, colon),
                libraryId.substring(colon + 1));
    }

    /** The code of a 3D model by its library path: {@code Package_SO.3dshapes/SOIC-8_3.9x4.9mm_P1.27mm} is {@code PACKAGE-SO:SOIC-8-3.9X4.9MM-P1.27MM:M3D}. */
    public static String modelCodeOf(String libraryPath) {
        int separator = libraryPath.indexOf(KiCadSources.MODEL_LIBRARY);
        return separator < 0 ? ImportedNames.code(TechnicalClass.MODEL, KiCadSymbolDrawing.KICAD, libraryPath)
                : ImportedNames.code(TechnicalClass.MODEL, libraryPath.substring(0, separator),
                libraryPath.substring(separator + KiCadSources.MODEL_LIBRARY.length()));
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
        return documentOf(libraryId, footprint, origin, libraries, Optional.empty());
    }

    /**
     * The same, standing with the bodies given in place of the models KiCad names: this library's own, where it draws the same land
     * pattern ({@link OwnBodies}).
     */
    static DocumentNode documentOf(String libraryId, KiCadFootprint footprint, KiCadFootprintLibraries.Origin origin, KiCadLibraries libraries,
                                   Optional<List<FootprintLines.Model>> bodies) {
        String said = footprint.description().isBlank() ? footprint.name() : footprint.description().strip();
        boolean installed = origin == KiCadFootprintLibraries.Origin.INSTALLED;
        return new ImportedFootprint(footprint.name(), slugOf(libraryId), codeOf(libraryId), said + " — imported from KiCad",
                provenanceOf(libraryId, footprint, installed, libraries), padsOf(footprint), outlinesOf(footprint, libraries), fieldsOf(footprint, libraries),
                bodies.orElseGet(() -> footprint.models().stream().map(KiCadFootprintDrawing::model).toList())).document(libraries);
    }

    /**
     * Whose an imported footprint is: what its own fields say, where they name a holder or a licence — a footprint that left
     * this program for KiCad comes back as whose it was, its source kept or, where it says none, where it was taken from now —;
     * else, of KiCad's installed libraries, the KiCad Libraries Team's under their licence, and of a project's own, the project's.
     */
    static Provenance provenanceOf(String libraryId, KiCadFootprint footprint, boolean installed, KiCadLibraries libraries) {
        String source = KiCadSources.footprint(libraryId, installed);
        return libraries.provenanceOf(footprint.properties())
                .map(said -> said.source() == null ? said.from(source) : said)
                .orElseGet(() -> libraries.provenance(source, installed));
    }

    /** The footprint's pads of copper or holes, as this library writes them. */
    /**
     * Where the placed part's reference and value are printed, as KiCad places them — by their middle, as a field is —; one
     * KiCad hides is not printed and is not carried. A field on a layer a field is not printed on stands where a field
     * stands when none is named.
     */
    static List<FootprintLines.Field> fieldsOf(KiCadFootprint footprint, KiCadLibraries libraries) {
        // KiCad's Y runs down, as ours does; ImportedFields reads a point with Y upwards.
        return footprint.fields().stream().filter(field -> !field.hidden())
                .map(field -> ImportedFields.placedAt(field.property() == KiCadProperty.REFERENCE ? PcbVocabulary.FIELD_REFERENCE : PcbVocabulary.FIELD_VALUE,
                        field.at().x(), -field.at().y(), field.height(), field.rotation(), acrossOf(field.across()), anchorOf(field.along()),
                        libraries.layerOf(field.layer()).filter(PcbVocabulary.FIELD_LAYERS::contains).orElse(null)))
                .toList();
    }

    /** Which part of the letters stands on a field's point along them, as KiCad justifies it. */
    static String anchorOf(KiCadPcb.Along along) {
        return switch (along) {
            case LEFT -> PcbVocabulary.TEXT_START;
            case CENTRE -> PcbVocabulary.TEXT_MIDDLE;
            case RIGHT -> PcbVocabulary.TEXT_END;
        };
    }

    /** How KiCad justifies a field whose letters stand on its point by this part of them: the way back of {@link #anchorOf}. */
    static KiCadPcb.Along alongOf(String anchor) {
        return switch (anchor == null ? PcbVocabulary.TEXT_MIDDLE : anchor) {
            case PcbVocabulary.TEXT_START -> KiCadPcb.Along.LEFT;
            case PcbVocabulary.TEXT_END -> KiCadPcb.Along.RIGHT;
            default -> KiCadPcb.Along.CENTRE;
        };
    }

    private static ImportedFields.Across acrossOf(KiCadPcb.Across across) {
        return switch (across) {
            case TOP -> ImportedFields.Across.TOP;
            case MIDDLE -> ImportedFields.Across.MIDDLE;
            case BOTTOM -> ImportedFields.Across.BOTTOM;
        };
    }

    static List<FootprintLines.Pad> padsOf(KiCadFootprint footprint) {
        return footprint.pads().stream().filter(KiCadFootprintDrawing::isCopperOrHole).map(KiCadFootprintDrawing::pad).toList();
    }

    /** A pad on no copper — a window in the paste — as what it is: an area on each layer it names. */
    private static List<KiCadFootprintGraphic> windowsOf(KiCadPad pad) {
        List<KiCadPoint> outline = outlineOf(pad);
        return pad.layers().stream().<KiCadFootprintGraphic>map(layer -> new KiCadFootprintGraphic.Polygon(outline, layer, 0, true)).toList();
    }

    /** A pad's outline on its footprint: its corners rounded as its shape says, turned as KiCad turns it. */
    private static List<KiCadPoint> outlineOf(KiCadPad pad) {
        double least = Math.min(pad.width(), pad.height());
        double radius = switch (pad.shape()) {
            case CIRCLE, OVAL -> least / 2;
            case ROUNDED_RECTANGLE -> least * pad.roundness();
            default -> 0;
        };
        double across = pad.width() / 2 - radius;
        double down = pad.height() / 2 - radius;
        double[][] corners = {{across, down}, {-across, down}, {-across, -down}, {across, -down}};
        double turn = Math.toRadians(pad.angle());
        List<KiCadPoint> outline = new ArrayList<>();
        for (int corner = 0; corner < corners.length; corner++) {
            for (int step = 0; step <= WINDOW_CORNER_STEPS; step++) {
                double angle = Math.PI / 2 * (corner + (double) step / WINDOW_CORNER_STEPS);
                double x = corners[corner][0] + radius * Math.cos(angle);
                double y = corners[corner][1] + radius * Math.sin(angle);
                outline.add(new KiCadPoint(pad.at().x() + x * Math.cos(turn) + y * Math.sin(turn),
                        pad.at().y() - x * Math.sin(turn) + y * Math.cos(turn)));
            }
        }
        return outline;
    }

    /** A pad on no copper — a window in the paste over a thermal pad — is no pad of a part: it joins nothing and is etched nowhere. */
    private static boolean isCopperOrHole(KiCadPad pad) {
        return pad.type() == KiCadPadType.UNPLATED_HOLE || pad.layers().stream().anyMatch(layer -> layer.endsWith(KiCadLayer.COPPER_SUFFIX));
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
                round.rounded(drill[1]), round.rounded(pad.roundness()), customOutlineOf(pad), faceOf(pad), opensOf(pad),
                pad.maskMargin().map(round::rounded).orElse(null), pad.pasteMargin().map(round::rounded).orElse(null));
    }

    /** A custom pad's copper as one path about its centre: its primitives; none for a pad of any other shape. */
    private static Optional<PathData> customOutlineOf(KiCadPad pad) {
        if (pad.shape() != KiCadPadShape.CUSTOM || pad.primitives().isEmpty()) {
            return Optional.empty();
        }
        RoundedPath path = new RoundedPath(PLACES);
        pad.primitives().forEach(primitive -> draw(primitive, path));
        return Optional.of(path.build());
    }

    /** A surface pad on the back copper alone, of a footprint read as it stands on the front, is on the part's far face. */
    private static String faceOf(KiCadPad pad) {
        boolean back = pad.layers().contains(KiCadSide.BACK.copper()) && !pad.layers().contains(KiCadSide.FRONT.copper());
        return pad.type() == KiCadPadType.SURFACE_MOUNT && back ? PcbVocabulary.FACE_FAR : "";
    }

    /** What a pad opens, by the layers it names; a hole of no copper says nothing. */
    private static String opensOf(KiCadPad pad) {
        if (pad.type() == KiCadPadType.UNPLATED_HOLE) {
            return "";
        }
        boolean mask = pad.isOn(KiCadLayer.MASK_SUFFIX);
        boolean paste = pad.isOn(KiCadLayer.PASTE_SUFFIX);
        return mask && paste ? PcbVocabulary.OPENS_BOTH : mask ? PcbVocabulary.OPENS_MASK : paste ? PcbVocabulary.OPENS_PASTE : PcbVocabulary.OPENS_NONE;
    }

    /** KiCad places a model as the line does — X right, Y up, Z out of the pcb —, so its numbers are carried as they are. */
    private static FootprintLines.Model model(KiCadModel model) {
        return new FootprintLines.Model(modelCodeOf(model.libraryPath()), axes(model.offset()), axes(model.rotation()),
                axes(model.scale()), model.hidden());
    }

    private static FootprintLines.Axes axes(KiCadModel.Axes axes) {
        RoundedPath round = new RoundedPath(PLACES);
        return new FootprintLines.Axes(round.rounded(axes.x()), round.rounded(axes.y()), round.rounded(axes.z()));
    }

    /** One outline for each layer, stroke and fill, its shapes in the order KiCad draws them. */
    private static List<FootprintLines.Outline> outlinesOf(KiCadFootprint footprint, KiCadLibraries libraries) {
        FootprintOutlines outlines = new FootprintOutlines();
        List<KiCadFootprintGraphic> graphics = new ArrayList<>(footprint.graphics());
        footprint.pads().stream().filter(pad -> !isCopperOrHole(pad)).map(KiCadFootprintDrawing::windowsOf).forEach(graphics::addAll);
        for (KiCadFootprintGraphic graphic : graphics) {
            libraries.layerOf(graphic.layer()).ifPresent(layer -> draw(graphic, outlines.path(layer, graphic.width(), graphic.filled())));
        }
        return outlines.outlines();
    }

    /** What a graphic draws, as path data. */
    static String pathOf(KiCadFootprintGraphic graphic) {
        RoundedPath path = new RoundedPath(PLACES);
        draw(graphic, path);
        return path.build().toSource();
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
            case KiCadFootprintGraphic.Curve curve -> path.curveFrom(curve.start().x(), curve.start().y(), curve.first().x(),
                    curve.first().y(), curve.second().x(), curve.second().y(), curve.end().x(), curve.end().y());
            case KiCadFootprintGraphic.Polygon polygon -> {
                if (!polygon.points().isEmpty()) {
                    path.moveTo(polygon.points().getFirst().x(), polygon.points().getFirst().y());
                    polygon.points().subList(1, polygon.points().size()).forEach(point -> path.lineTo(point.x(), point.y()));
                    path.close();
                }
            }
        }
    }
}
