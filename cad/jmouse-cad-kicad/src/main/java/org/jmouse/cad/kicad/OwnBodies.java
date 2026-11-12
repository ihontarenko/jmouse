package org.jmouse.cad.kicad;

import org.jmouse.cad.pcb.FootprintLines;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * A footprint a board carries, stood with this library's own 3D bodies where this library draws the same land pattern: the board's copper
 * is the board's — it may have been changed there —, the body is ours, drawn by this library, so no other library's model is copied in.
 * The same land pattern is every pad of ours where the board's of that number stands, all shifted alike; a footprint turned, or with a
 * pad more or less, is not the same, and keeps the models it names.
 */
public final class OwnBodies {

    /** How far a pad may stand from where ours puts it and still be the same land pattern, in millimetres. */
    private static final double SAME_PLACE = 0.05;

    private OwnBodies() {
    }

    /**
     * Our footprint's models, each moved by the shift between our land pattern and the board's, in the model's frame — X right, Y up
     * the drawing, as a pad's Y runs down it —; empty where the patterns differ, or ours stands with no model.
     */
    public static Optional<List<FootprintLines.Model>> of(List<FootprintLines.Pad> boards, FootprintDrawing ours) {
        if (ours.models().isEmpty() || boards.isEmpty() || boards.size() != ours.pads().size()) {
            return Optional.empty();
        }
        Map<String, FootprintLines.Pad> byNumber = boards.stream().collect(Collectors.toMap(FootprintLines.Pad::number, Function.identity(), (first, second) -> first));
        FootprintDrawing.Pad anchor = ours.pads().getFirst();
        FootprintLines.Pad standing = byNumber.get(anchor.number());
        if (standing == null) {
            return Optional.empty();
        }
        double dx = standing.x() - anchor.x();
        double dy = standing.y() - anchor.y();
        for (FootprintDrawing.Pad pad : ours.pads()) {
            FootprintLines.Pad theirs = byNumber.get(pad.number());
            if (theirs == null || Math.hypot(theirs.x() - pad.x() - dx, theirs.y() - pad.y() - dy) > SAME_PLACE) {
                return Optional.empty();
            }
        }
        return Optional.of(ours.models().stream().map(model -> new FootprintLines.Model(model.reference(),
                new FootprintLines.Axes(model.offset().get(0) + dx, model.offset().get(1) - dy, model.offset().get(2)),
                axes(model.rotation()), axes(model.scale()), model.hidden())).toList());
    }

    private static FootprintLines.Axes axes(List<Double> values) {
        return new FootprintLines.Axes(values.get(0), values.get(1), values.get(2));
    }
}
