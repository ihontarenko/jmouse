package org.jmouse.cad.libre.pcb;

import org.jmouse.libre.pcb.LibrePcbPlaceholder;
import org.jmouse.cad.exchange.imported.ImportedFields;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.cad.exchange.imported.FootprintOutlines;
import org.jmouse.cad.exchange.imported.ImportedFootprint;
import org.jmouse.cad.exchange.imported.RoundedPath;
import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.svg.path.node.PathData;
import org.jmouse.libre.pcb.LibrePcbBoard;
import org.jmouse.libre.pcb.LibrePcbGraphic;
import org.jmouse.libre.pcb.LibrePcbLayer;
import org.jmouse.libre.pcb.LibrePcbLibrary;
import org.jmouse.libre.pcb.LibrePcbOpening;
import org.jmouse.libre.pcb.LibrePcbPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A footprint of a package as a jM footprint. A footprint's layers are drawn as seen from above, so what stands on its
 * bottom layers is not among them. What a pad opens in the mask and the paste comes from the pad, and from the board's rules
 * where it leaves it to them: they are what LibrePCB itself makes the pad with.
 */
final class LibrePcbFootprintDrawing {

    private static final int PLACES = FootprintOutlines.PLACES;

    /** LibrePCB's radius of 1 is corners rounded all the way; ours is a share of the shorter side, so half of it. */
    private static final double ROUNDNESS_SCALE = 0.5;

    private LibrePcbFootprintDrawing() {
    }

    static DocumentNode documentOf(LibrePcbLibrary.Package found, LibrePcbLibrary.Footprint footprint, Optional<LibrePcbBoard.DesignRules> rules,
                                   LibrePcbFiling filing) {
        String said = found.description().isBlank() ? found.name() : found.description().strip().lines().findFirst().orElse(found.name());
        List<FootprintLines.Pad> pads = new ArrayList<>();
        footprint.pads().forEach(pad -> pads.add(pad(pad, found, rules)));
        footprint.holes().forEach(hole -> pads.add(hole(hole)));
        return new ImportedFootprint(found.name(), LibrePcbNames.footprint(found, footprint), LibrePcbNames.footprintCode(found, footprint),
                said + " — imported from LibrePCB",
                filing.provenance(LibrePcbNames.source(LibrePcbNames.SourceKind.PACKAGE, found.name(), found.id()), found.author()), pads,
                outlinesOf(footprint.graphics(), filing), fieldsOf(footprint.graphics()), modelsOf(found, footprint)).document(filing);
    }

    /** The models it stands with, each placed as the footprint says: LibrePCB's axes are the line's, X right, Y up, Z out of the pcb. */
    private static List<FootprintLines.Model> modelsOf(LibrePcbLibrary.Package found, LibrePcbLibrary.Footprint footprint) {
        return footprint.models().stream().map(model -> new FootprintLines.Model(LibrePcbNames.modelCode(found, model), axes(footprint.modelPosition()),
                axes(footprint.modelRotation()), FootprintLines.Axes.ONE, false)).toList();
    }

    private static FootprintLines.Axes axes(LibrePcbLibrary.Axes axes) {
        RoundedPath round = new RoundedPath(PLACES);
        return new FootprintLines.Axes(round.rounded(axes.x()), round.rounded(axes.y()), round.rounded(axes.z()));
    }

    private static FootprintLines.Pad pad(LibrePcbLibrary.FootprintPad pad, LibrePcbLibrary.Package found, Optional<LibrePcbBoard.DesignRules> rules) {
        RoundedPath round = new RoundedPath(PLACES);
        String number = pad.packagePad().map(id -> found.padNames().getOrDefault(id, "")).orElse("");
        double shorterSide = Math.min(pad.width(), pad.height());
        double[] drill = drillOf(pad);
        return new FootprintLines.Pad(number, pad.goesThrough() ? PcbVocabulary.THROUGH_HOLE : PcbVocabulary.SURFACE, shapeOf(pad), round.rounded(pad.at().x()),
                round.rounded(-pad.at().y()), round.rounded(pad.rotation()), round.rounded(pad.width()), round.rounded(pad.height()),
                round.rounded(drill[0]), round.rounded(drill[1]), round.rounded(pad.radius() * ROUNDNESS_SCALE), outlineOf(pad),
                pad.onBottom() && !pad.goesThrough() ? PcbVocabulary.FACE_FAR : "", opensOf(pad), maskMarginOf(pad, shorterSide, rules, round), pasteMarginOf(pad, shorterSide, rules, round));
    }

    /** A pad of a radius all the way round is a circle or an oval to us, one of none a rectangle. */
    private static String shapeOf(LibrePcbLibrary.FootprintPad pad) {
        return switch (pad.shape()) {
            case CUSTOM -> PcbVocabulary.CUSTOM;
            // An octagon is a round ring as far as a pcb's copper goes, as it is for Eagle's.
            case OCTAGON -> PcbVocabulary.CIRCLE;
            case ROUNDED_RECTANGLE -> {
                if (pad.radius() <= 0) {
                    yield PcbVocabulary.RECTANGLE;
                }
                if (pad.radius() < 1) {
                    yield PcbVocabulary.ROUNDED_RECTANGLE;
                }
                yield pad.width() == pad.height() ? PcbVocabulary.CIRCLE : PcbVocabulary.OVAL;
            }
        };
    }

    private static Optional<PathData> outlineOf(LibrePcbLibrary.FootprintPad pad) {
        if (pad.shape() != LibrePcbLibrary.PadShape.CUSTOM || pad.vertices().isEmpty()) {
            return Optional.empty();
        }
        RoundedPath path = new RoundedPath(PLACES);
        LibrePcbPaths.corners(pad.vertices(), path, point -> new double[] {point.x(), -point.y()});
        return Optional.of(path.build());
    }

    /** A pad's drill, a slot as wide as its run: LibrePCB runs a slot between its hole's corners, its diameter round them. */
    private static double[] drillOf(LibrePcbLibrary.FootprintPad pad) {
        if (!pad.goesThrough()) {
            return new double[] {0, 0};
        }
        LibrePcbLibrary.Hole hole = pad.holes().getFirst();
        LibrePcbPoint first = hole.vertices().getFirst().at();
        LibrePcbPoint last = hole.vertices().getLast().at();
        return new double[] {hole.diameter() + Math.abs(last.x() - first.x()), hole.diameter() + Math.abs(last.y() - first.y())};
    }

    private static String opensOf(LibrePcbLibrary.FootprintPad pad) {
        boolean mask = !(pad.stopMask() instanceof LibrePcbOpening.Off);
        // Only a pad on the surface takes paste when it leaves that to the rules.
        boolean paste = switch (pad.solderPaste()) {
            case LibrePcbOpening.Off ignored -> false;
            case LibrePcbOpening.Automatic ignored -> !pad.goesThrough();
            case LibrePcbOpening.Clearance ignored -> true;
        };
        return mask && paste ? PcbVocabulary.OPENS_BOTH : mask ? PcbVocabulary.OPENS_MASK : paste ? PcbVocabulary.OPENS_PASTE : PcbVocabulary.OPENS_NONE;
    }

    private static Double maskMarginOf(LibrePcbLibrary.FootprintPad pad, double shorterSide, Optional<LibrePcbBoard.DesignRules> rules, RoundedPath round) {
        return switch (pad.stopMask()) {
            case LibrePcbOpening.Off ignored -> null;
            case LibrePcbOpening.Clearance own -> round.rounded(own.millimetres());
            case LibrePcbOpening.Automatic ignored -> rules.map(given -> round.rounded(given.maskClearance().of(shorterSide))).orElse(null);
        };
    }

    /** LibrePCB's paste clearance shrinks the paste; our margin grows it, so its sign turns. */
    private static Double pasteMarginOf(LibrePcbLibrary.FootprintPad pad, double shorterSide, Optional<LibrePcbBoard.DesignRules> rules, RoundedPath round) {
        return switch (pad.solderPaste()) {
            case LibrePcbOpening.Off ignored -> null;
            case LibrePcbOpening.Clearance own -> round.rounded(-own.millimetres());
            case LibrePcbOpening.Automatic ignored -> rules.map(given -> round.rounded(-given.pasteClearance().of(shorterSide))).orElse(null);
        };
    }

    /** A hole with no copper round it, numbered by nothing: no pin lands on it. */
    private static FootprintLines.Pad hole(LibrePcbLibrary.Hole hole) {
        RoundedPath round = new RoundedPath(PLACES);
        LibrePcbPoint at = hole.vertices().getFirst().at();
        double drill = round.rounded(hole.diameter());
        return new FootprintLines.Pad("", PcbVocabulary.UNPLATED_HOLE, PcbVocabulary.CIRCLE, round.rounded(at.x()), round.rounded(-at.y()), 0, drill, drill, drill, drill,
                0, Optional.empty());
    }

    /**
     * Where the placed part's name and value are printed: the top's texts that are {@code {{NAME}}} or {@code {{VALUE}}} and
     * nothing else, by the point and the alignment LibrePCB gives them ({@link ImportedFields}). LibrePCB keeps them on layers of their own,
     * none of which is what is made, so each is printed where a field is printed when none is named.
     */
    private static List<FootprintLines.Field> fieldsOf(List<LibrePcbGraphic> graphics) {
        List<FootprintLines.Field> fields = new ArrayList<>();
        for (LibrePcbGraphic graphic : graphics) {
            if (graphic instanceof LibrePcbGraphic.Text text && !LibrePcbLayer.isBottom(text.layer())) {
                String subject = LibrePcbPlaceholder.NAME.isAll(text.value()) ? PcbVocabulary.FIELD_REFERENCE
                        : LibrePcbPlaceholder.VALUE.isAll(text.value()) ? PcbVocabulary.FIELD_VALUE : null;
                if (subject != null) {
                    fields.add(ImportedFields.placedAt(subject, text.at().x(), text.at().y(), text.height(), text.rotation(), acrossOf(text.across()),
                            anchorOf(text.alignment()), null));
                }
            }
        }
        return fields;
    }

    private static String anchorOf(LibrePcbGraphic.Alignment alignment) {
        return switch (alignment) {
            case LEFT -> PcbVocabulary.TEXT_START;
            case CENTRE -> PcbVocabulary.TEXT_MIDDLE;
            case RIGHT -> PcbVocabulary.TEXT_END;
        };
    }

    private static ImportedFields.Across acrossOf(LibrePcbGraphic.Across across) {
        return switch (across) {
            case TOP -> ImportedFields.Across.TOP;
            case MIDDLE -> ImportedFields.Across.MIDDLE;
            case BOTTOM -> ImportedFields.Across.BOTTOM;
        };
    }

    private static List<FootprintLines.Outline> outlinesOf(List<LibrePcbGraphic> graphics, LibrePcbFiling filing) {
        FootprintOutlines outlines = new FootprintOutlines();
        for (LibrePcbGraphic graphic : graphics) {
            if (LibrePcbLayer.isBottom(graphic.layer())) {
                continue;
            }
            filing.layerOf(graphic.layer()).ifPresent(layer -> draw(graphic, layer, outlines));
        }
        return outlines.outlines();
    }

    private static void draw(LibrePcbGraphic graphic, String layer, FootprintOutlines outlines) {
        switch (graphic) {
            case LibrePcbGraphic.Polygon polygon -> LibrePcbPaths.corners(polygon.vertices(), outlines.path(layer, polygon.width(), polygon.filled()),
                    point -> new double[] {point.x(), -point.y()});
            case LibrePcbGraphic.Circle circle -> outlines.path(layer, circle.width(), circle.filled())
                    .circle(circle.centre().x(), -circle.centre().y(), circle.diameter() / 2);
            // Words on a footprint are its name and its value: they are its fields ({@link #fieldsOf}), not lines.
            case LibrePcbGraphic.Text ignored -> {
            }
        }
    }
}
