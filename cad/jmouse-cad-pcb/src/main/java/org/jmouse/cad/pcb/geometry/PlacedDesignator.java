package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Reference;
import org.jmouse.geometry.index.Box;

import org.jmouse.settings.Bounds;
import org.jmouse.settings.Label;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.svg.path.node.PathPolylines;
import org.jmouse.svg.path.parser.PathParser;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where a part's designator stands on the pcb: its point — which part of the letters stands on it, the field's anchor
 * says —, its letters' height and its turn as seen, counter-clockwise. Where the part's own line moved it, there; else
 * where its footprint's reference field stands; else centred above the part, sized by its width.
 */
public record PlacedDesignator(double x, double y, double height, double rotation, String layer, String anchor) {

    /** Whether it is printed on the pcb, rather than drawn for the assembly alone. */
    public boolean printed() {
        return PcbVocabulary.SILKSCREEN.equals(layer);
    }

    /** How an outline's curves are followed to measure it: as finely as a drawn one is. */
    private static final int CURVE_STEPS = 8;
    private static final PathParser PATHS = new PathParser();

    /**
     * How a designator is set where nothing says where: the gap above the part, the least and most its letters may be,
     * and the share of the part's width they take; and how far each letter advances, as a share of its height.
     *
     * @param gap        how far above the part's box its designator stands
     * @param least      the least height its letters are written at
     * @param most       the most
     * @param widthShare the share of the part's width its letters are high, between those
     * @param advance    how far each letter advances, as a share of its height
     */
    public record Rules(@Label(value = "Gap", unit = "mm") double gap,
                        @Label(value = "Least height", unit = "mm") double least,
                        @Label(value = "Most height", unit = "mm") double most,
                        @Bounds(minimum = 0, maximum = 1, step = 0.01) @Label("Width share") double widthShare,
                        @Label("Advance") double advance) {
    }

    /**
     * The box the designator's letters cover, a part named {@code name}: as wide as its letters advance, as tall as they
     * are, standing on the point as its anchor says, upright or turned a quarter.
     */
    public Box bounds(String name, Rules rules) {
        double along = name.length() * height * rules.advance();
        boolean upright = ((rotation % 180) + 180) % 180 != 90;
        double width = upright ? along : height;
        double tall = upright ? height : along;
        double shift = PcbVocabulary.TEXT_START.equals(anchor) ? along / 2 : PcbVocabulary.TEXT_END.equals(anchor) ? -along / 2 : 0;
        double turn = Math.toRadians(rotation);
        double middleX = x + shift * Math.cos(turn);
        double middleY = y - shift * Math.sin(turn);
        return new Box(middleX - width / 2, middleY - tall / 2, middleX + width / 2, middleY + tall / 2);
    }

    public static PlacedDesignator of(Placement placement, Optional<FootprintDrawing> footprint, Rules rules) {
        Optional<FootprintDrawing.Field> field = footprint.flatMap(drawing -> drawing.fields().stream()
                .filter(candidate -> PcbVocabulary.FIELD_REFERENCE.equals(candidate.subject())).findFirst());
        String anchor = field.map(FootprintDrawing.Field::anchor).orElse(PcbVocabulary.TEXT_MIDDLE);
        if (placement.reference().isPresent()) {
            Reference moved = placement.reference().get();
            double height = field.map(FootprintDrawing.Field::height).orElse(rules.least());
            Point at = placement.placed(moved.x(), moved.y());
            String layer = moved.layer() == null || moved.layer().isEmpty() ? field.map(FootprintDrawing.Field::layer).orElse(PcbVocabulary.SILKSCREEN) : moved.layer();
            return new PlacedDesignator(at.x(), at.y(), height, moved.rotation() + placement.rotation(), layer, anchor);
        }
        if (field.isPresent()) {
            Point at = placement.placed(field.get().x(), field.get().y());
            return new PlacedDesignator(at.x(), at.y(), field.get().height(), field.get().rotation() + placement.rotation(), field.get().layer(), anchor);
        }
        double[] box = bounds(placement, footprint);
        double width = box[2] - box[0];
        double height = Math.max(rules.least(), Math.min(rules.most(), width * rules.widthShare()));
        return new PlacedDesignator(box[0] + width / 2, box[1] - rules.gap() - height / 2, height, 0, PcbVocabulary.SILKSCREEN,
                PcbVocabulary.TEXT_MIDDLE);
    }

    /**
     * The box a placed part takes — left, top, right, bottom —: its footprint's pads and every outline it draws — its body on
     * the silkscreen, its courtyard —, turned with it, as the interface measures a part ({@code footprintExtent}); a part
     * with no footprint yet a 2 mm square at its place.
     */
    private static double[] bounds(Placement placement, Optional<FootprintDrawing> footprint) {
        List<double[]> corners = new ArrayList<>();
        footprint.ifPresent(drawing -> {
            for (FootprintDrawing.Pad pad : drawing.pads()) {
                double reach = Math.hypot(pad.width(), pad.height()) / 2;
                corners.add(new double[] {pad.x() - reach, pad.y() - reach});
                corners.add(new double[] {pad.x() + reach, pad.y() + reach});
            }
            for (FootprintDrawing.Outline outline : drawing.outlines()) {
                if (outline.path() == null || outline.path().isBlank()) {
                    continue;
                }
                for (List<double[]> run : PathPolylines.of(PATHS.parse(outline.path()), CURVE_STEPS)) {
                    corners.addAll(run);
                }
            }
        });
        if (corners.isEmpty()) {
            corners.add(new double[] {-1, -1});
            corners.add(new double[] {1, 1});
        }
        double left = corners.stream().mapToDouble(point -> point[0]).min().orElse(0);
        double right = corners.stream().mapToDouble(point -> point[0]).max().orElse(0);
        double top = corners.stream().mapToDouble(point -> point[1]).min().orElse(0);
        double bottom = corners.stream().mapToDouble(point -> point[1]).max().orElse(0);
        double[] box = {Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (double[] corner : List.of(new double[] {left, top}, new double[] {right, top}, new double[] {left, bottom},
                new double[] {right, bottom})) {
            Point placed = placement.placed(corner[0], corner[1]);
            box[0] = Math.min(box[0], placed.x());
            box[1] = Math.min(box[1], placed.y());
            box[2] = Math.max(box[2], placed.x());
            box[3] = Math.max(box[3], placed.y());
        }
        return box;
    }
}
