package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.geometry.CopperShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jmouse.cad.document.FindingSeverity.WARNING;
import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/** Track ends that reach no pad, via or other track on their layer: copper left hanging. */
final class LooseEndCheck implements PcbCheck {

    /** How many places a loose end's point is said to. */
    private static final int PLACES = 2;

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        PcbDrawing pcb = context.pcb();
        List<PcbFinding> findings = new ArrayList<>();
        for (int index = 0; index < pcb.tracks().size(); index++) {
            Track track = pcb.tracks().get(index);
            String own = key("track", index);
            for (Point end : List.of(track.points().getFirst(), track.points().getLast())) {
                CopperShape point = CopperShape.circle(end.x(), end.y(), 0);
                boolean reached = Arrays.stream(context.piecesNear().near(point.box(), 0)).mapToObj(context.pieces()::get)
                        .anyMatch(piece -> !piece.key().equals(own) && piece.onLayer().test(track.layer())
                        && piece.shape().within(point, 0) && piece.shape().comesWithin(point, context.settings().tolerance()));
                if (!reached) {
                    findings.add(new PcbFinding(WARNING, "Track " + (index + 1) + " ends at " + FindingWords.written(FindingWords.rounded(end.x(), PLACES))
                            + ", " + FindingWords.written(FindingWords.rounded(end.y(), PLACES)) + " joined to nothing", List.of(own), end));
                }
            }
        }
        return findings;
    }
}
