package org.jmouse.cad.pcb.checks;

import org.jmouse.cad.pcb.drawing.Keepout;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.CopperShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.jmouse.cad.document.FindingSeverity.ERROR;
import static org.jmouse.cad.pcb.geometry.CopperPieces.key;

/** Copper standing in a keepout, on a layer the keepout keeps clear. */
final class KeepoutCheck implements PcbCheck {

    @Override
    public List<PcbFinding> findings(CheckContext context) {
        PcbDrawing pcb = context.pcb();
        List<PcbFinding> findings = new ArrayList<>();
        for (int index = 0; index < pcb.keepouts().size(); index++) {
            Keepout keepout = pcb.keepouts().get(index);
            Optional<CopperShape> area = CopperShape.ofPath(keepout.path(), true, 0, context.settings().curveSteps());
            if (area.isEmpty()) {
                continue;
            }
            List<CopperPieces.Piece> inside = Arrays.stream(context.piecesNear().near(area.get().box(), 0)).mapToObj(context.pieces()::get)
                    .filter(piece -> keepout.layers().stream().anyMatch(piece.onLayer())
                    && piece.shape().within(area.get(), 0) && piece.shape().comesWithin(area.get(), 0)).toList();
            if (!inside.isEmpty()) {
                List<String> keys = new ArrayList<>(List.of(key("keepout", index)));
                inside.forEach(piece -> keys.add(piece.key()));
                findings.add(new PcbFinding(ERROR, (inside.size() == 1 ? inside.getFirst().label() + " stands" : inside.size() + " pieces of copper stand")
                        + " in keepout " + (index + 1), keys));
            }
        }
        return findings;
    }
}
