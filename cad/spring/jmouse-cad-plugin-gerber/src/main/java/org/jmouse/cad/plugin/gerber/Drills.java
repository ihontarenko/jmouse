package org.jmouse.cad.plugin.gerber;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.jmouse.cad.board.drawing.FootprintDrawing;
import org.jmouse.gerber.excellon.compose.DrillHole;
import org.jmouse.gerber.excellon.compose.ExcellonComposer;

import java.util.List;

/**
 * The board's holes as Excellon: vias, plated mounting holes and through-hole pads plated, unplated holes not — a hole
 * longer one way than the other is a slot, cut along its centre line.
 */
final class Drills {

    /** A hole's two sides closer than this are one: a round hole, not a slot. */
    private static final double SAME = 1e-6;

    /** The plated holes and the unplated ones: the board house plates one file and not the other. */
    record Programs(String plated, String unplated) {
    }

    private Drills() {
    }

    static Programs of(BoardFabrication fabrication) {
        ExcellonComposer plated = new ExcellonComposer(true);
        ExcellonComposer unplated = new ExcellonComposer(false);
        for (BoardDrawing.Via via : fabrication.board().vias()) {
            plated.hole(via.drill(), new DrillHole.Round(BoardGerber.out(via.x(), via.y())));
        }
        for (BoardDrawing.Hole hole : fabrication.board().holes()) {
            (hole.ring() > 0 ? plated : unplated).hole(hole.drill(), new DrillHole.Round(BoardGerber.out(hole.x(), hole.y())));
        }
        for (BoardFabrication.SeatedPart part : fabrication.parts()) {
            for (FootprintDrawing.Pad pad : part.drawing().map(FootprintDrawing::pads).orElse(List.of())) {
                boolean throughHole = pad.isPlatedThrough();
                if (pad.drillWidth() > 0 && (throughHole || pad.isUnplated())) {
                    drill(throughHole ? plated : unplated, part.placement(), pad);
                }
            }
        }
        return new Programs(plated.program().toSource(), unplated.program().toSource());
    }

    private static void drill(ExcellonComposer composer, BoardDrawing.Placement placement, FootprintDrawing.Pad pad) {
        double height = pad.drillHeight() > 0 ? pad.drillHeight() : pad.drillWidth();
        double narrow = Math.min(pad.drillWidth(), height);
        double wide = Math.max(pad.drillWidth(), height);
        if (wide - narrow < SAME) {
            composer.hole(narrow, new DrillHole.Round(BoardGerber.placedOut(placement, pad.x(), pad.y())));
            return;
        }
        // A slot: its centre line along its longer side, as long as that side less the width the tool cuts.
        double reach = (wide - narrow) / 2;
        double turn = Math.toRadians(pad.rotation());
        boolean alongWidth = pad.drillWidth() >= height;
        double alongX = alongWidth ? reach * Math.cos(turn) : reach * Math.sin(turn);
        double alongY = alongWidth ? -reach * Math.sin(turn) : reach * Math.cos(turn);
        composer.hole(narrow, new DrillHole.Slot(BoardGerber.placedOut(placement, pad.x() - alongX, pad.y() - alongY),
                                                 BoardGerber.placedOut(placement, pad.x() + alongX, pad.y() + alongY)));
    }
}
