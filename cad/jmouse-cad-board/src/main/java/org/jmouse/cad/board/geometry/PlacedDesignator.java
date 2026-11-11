package org.jmouse.cad.board.geometry;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Where a part's designator stands on the board: its middle, its letters' height and its turn as seen, counter-clockwise.
 * Where the part's own line moved it, there; else where its footprint's reference field stands; else above the part,
 * sized by its width.
 */
public record PlacedDesignator(double x, double y, double height, double rotation) {

    private static final String REFERENCE = "reference";

    /**
     * How a designator is set where nothing says where: the gap above the part, the least and most its letters may be,
     * and the share of the part's width they take.
     */
    public record Rules(double gap, double least, double most, double widthShare) {
    }

    public static PlacedDesignator of(BoardDrawing.Placement placement, Optional<FootprintDrawing> footprint, Rules rules) {
        Optional<FootprintDrawing.Field> field = footprint.flatMap(drawing -> drawing.fields().stream()
                .filter(candidate -> REFERENCE.equals(candidate.subject())).findFirst());
        if (placement.reference().isPresent()) {
            BoardDrawing.Reference moved = placement.reference().get();
            double height = field.map(FootprintDrawing.Field::height).orElse(rules.least());
            BoardDrawing.Point at = placement.placed(moved.x(), moved.y());
            return new PlacedDesignator(at.x(), at.y(), height, moved.rotation() + placement.rotation());
        }
        if (field.isPresent()) {
            BoardDrawing.Point at = placement.placed(field.get().x(), field.get().y());
            return new PlacedDesignator(at.x(), at.y(), field.get().height(), field.get().rotation() + placement.rotation());
        }
        double[] box = bounds(placement, footprint);
        double width = box[2] - box[0];
        double height = Math.max(rules.least(), Math.min(rules.most(), width * rules.widthShare()));
        return new PlacedDesignator(box[0] + width / 2, box[1] - rules.gap() - height / 2, height, 0);
    }

    /**
     * The box a placed part takes — left, top, right, bottom —: its footprint's pads turned with it; a part
     * with no footprint yet a 2 mm square at its place.
     */
    private static double[] bounds(BoardDrawing.Placement placement, Optional<FootprintDrawing> footprint) {
        List<double[]> corners = new ArrayList<>();
        footprint.ifPresent(drawing -> {
            for (FootprintDrawing.Pad pad : drawing.pads()) {
                double reach = Math.hypot(pad.width(), pad.height()) / 2;
                corners.add(new double[] {pad.x() - reach, pad.y() - reach});
                corners.add(new double[] {pad.x() + reach, pad.y() + reach});
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
            BoardDrawing.Point placed = placement.placed(corner[0], corner[1]);
            box[0] = Math.min(box[0], placed.x());
            box[1] = Math.min(box[1], placed.y());
            box[2] = Math.max(box[2], placed.x());
            box[3] = Math.max(box[3], placed.y());
        }
        return box;
    }
}
