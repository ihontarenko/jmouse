package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Hole;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every hole cut through a pcb, where it is on the pcb: its vias and plated mounting holes and its through-hole pads
 * plated, its unplated holes not. A hole longer one way than the other is a slot, cut along its centre line. What a pcb
 * house drills and what a model of the made pcb is cut through are the same holes.
 */
public final class PcbDrills {

    /** A hole's two sides closer than this are one: a round hole, not a slot. */
    private static final double SAME = 1e-6;

    /**
     * One hole: a round one where {@code from} and {@code to} are one point, else a slot cut from one to the other.
     *
     * @param width the tool's width, millimetres
     */
    public record Drill(boolean plated, double width, Point from, Point to) {

        public boolean round() {
            return from.equals(to);
        }
    }

    private PcbDrills() {
    }

    /** @param footprints each seated part's footprint drawing, by designator; a part without one has no holes */
    public static List<Drill> of(PcbDrawing pcb, Map<String, FootprintDrawing> footprints) {
        List<Drill> drills = new ArrayList<>();
        for (Via via : pcb.vias()) {
            drills.add(round(true, via.drill(), new Point(via.x(), via.y())));
        }
        for (Hole hole : pcb.holes()) {
            drills.add(round(hole.ring() > 0, hole.drill(), new Point(hole.x(), hole.y())));
        }
        for (Placement placement : pcb.placements()) {
            FootprintDrawing footprint = footprints.get(placement.designator());
            if (footprint == null) {
                continue;
            }
            for (FootprintDrawing.Pad pad : footprint.pads()) {
                if (pad.drillWidth() > 0 && (pad.isPlatedThrough() || pad.isUnplated())) {
                    drills.add(padDrill(placement, pad));
                }
            }
        }
        return List.copyOf(drills);
    }

    private static Drill round(boolean plated, double width, Point at) {
        return new Drill(plated, width, at, at);
    }

    private static Drill padDrill(Placement placement, FootprintDrawing.Pad pad) {
        boolean plated = pad.isPlatedThrough();
        double height = pad.drillHeight() > 0 ? pad.drillHeight() : pad.drillWidth();
        double narrow = Math.min(pad.drillWidth(), height);
        double wide = Math.max(pad.drillWidth(), height);
        if (wide - narrow < SAME) {
            return round(plated, narrow, placement.placed(pad.x(), pad.y()));
        }
        // A slot: its centre line along its longer side, as long as that side less the width the tool cuts.
        double reach = (wide - narrow) / 2;
        double turn = Math.toRadians(pad.rotation());
        boolean alongWidth = pad.drillWidth() >= height;
        double alongX = alongWidth ? reach * Math.cos(turn) : reach * Math.sin(turn);
        double alongY = alongWidth ? -reach * Math.sin(turn) : reach * Math.cos(turn);
        return new Drill(plated, narrow, placement.placed(pad.x() - alongX, pad.y() - alongY),
                placement.placed(pad.x() + alongX, pad.y() + alongY));
    }
}
