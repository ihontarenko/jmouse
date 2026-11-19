package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.List;

/**
 * One situation in which copper drawn as several pieces is one line, and how it is merged into one: a sliver knotting a joint
 * ({@link TrackSlivers}), a jog along a line ({@link LineJogs}), a line drawn along another of its net ({@link CoincidentStretches}).
 * Each knows its situation alone; which are asked, and in what order, is the caller's ({@link #inOrder}).
 */
interface LineMerging {

    /** The pcb merged, and where each track chosen stands in it now: a merging may take tracks away and add pieces. */
    record Merged(PcbDrawing pcb, List<Integer> chosen) {
    }

    /** The pcb with this situation merged around the chosen tracks; none chosen, everywhere. Where it is not found, the pcb as it is. */
    Merged merged(PcbDrawing pcb, List<Integer> chosen);

    /** Several mergings one after another, each over what the one before left — an untied knot then a plain joint for the next. */
    static LineMerging inOrder(List<LineMerging> mergings) {
        return (pcb, chosen) -> {
            Merged merged = new Merged(pcb, chosen);
            for (LineMerging merging : mergings) {
                merged = merging.merged(merged.pcb(), merged.chosen());
            }
            return merged;
        };
    }
}
