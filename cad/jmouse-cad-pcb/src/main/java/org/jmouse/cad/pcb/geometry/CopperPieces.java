package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Every piece of copper of a pcb whose nets are worked out, in one list, as everything that measures copper wants it: its
 * pads, tracks, arcs, copper drawn as shapes, vias, plated mounting holes — copper of no net — and the drops that widen
 * tracks into their pads, each its track's copper. A kind of copper is added here once, and every measure has it.
 */
public final class CopperPieces {

    private CopperPieces() {
    }

    /** What a piece of copper is. */
    public enum Kind {
        PAD, TRACK, ARC, DRAWN, VIA, HOLE, DROP
    }

    /**
     * One piece of copper.
     *
     * @param index   its place among its kind; a drop's is its track's
     * @param key     what a finding or a choice names it by: {@code track:3}
     * @param label   how a person names it: {@code R1 pad 2}, {@code Track 4}
     * @param part    the part a pad belongs to — two pads of one part are its footprint's to keep apart, not the pcb's —;
     *                empty for any other copper
     * @param onLayer whether it has copper on a layer
     * @param net     its net; empty for copper on none, or on two
     * @param group   which joined piece it is part of: two of one number are one conductor
     */
    public record Piece(Kind kind, int index, String key, String label, Optional<String> part, CopperShape shape,
                        Predicate<String> onLayer, Optional<String> net, int group) {
    }

    /** The pieces of a pcb, given its nets, its teardrops and how finely curves are followed. */
    public static List<Piece> of(PcbDrawing pcb, CopperNets nets, List<Teardrops.Drop> drops, int curveSteps) {
        List<Piece> pieces = new ArrayList<>();
        Map<String, Integer> placements = new HashMap<>();
        for (int index = 0; index < pcb.placements().size(); index++) {
            placements.put(pcb.placements().get(index).designator(), index);
        }
        for (int index = 0; index < nets.pads().size(); index++) {
            CopperNets.PlacedPad pad = nets.pads().get(index);
            Integer placement = placements.get(pad.designator());
            if (placement == null) {
                continue;
            }
            pieces.add(new Piece(Kind.PAD, index, key("placement", placement), pad.placement().printed() + " pad " + pad.pad().number(),
                    Optional.of(pad.designator()), pad.copper(), pad::on, nets.padNets().get(index), nets.padGroup(index)));
        }
        for (int index = 0; index < pcb.tracks().size(); index++) {
            PcbDrawing.Track track = pcb.tracks().get(index);
            pieces.add(new Piece(Kind.TRACK, index, key("track", index), "Track " + (index + 1), Optional.empty(),
                    CopperShapes.track(track, curveSteps), track.layer()::equals, nets.trackNets().get(index), nets.trackGroup(index)));
        }
        for (int index = 0; index < pcb.arcs().size(); index++) {
            PcbDrawing.Arc arc = pcb.arcs().get(index);
            pieces.add(new Piece(Kind.ARC, index, key("arc", index), "Arc " + (index + 1), Optional.empty(),
                    CopperShapes.arc(arc, curveSteps), arc.layer()::equals, nets.arcNets().get(index), nets.arcGroup(index)));
        }
        for (int index = 0; index < nets.drawn().size(); index++) {
            CopperShapes.Drawn drawn = nets.drawn().get(index);
            pieces.add(new Piece(Kind.DRAWN, index, key("shape", drawn.index()), "Shape " + (drawn.index() + 1), Optional.empty(),
                    drawn.shape(), drawn.layer()::equals, nets.drawnNets().get(index), nets.drawnGroup(index)));
        }
        for (int index = 0; index < pcb.vias().size(); index++) {
            PcbDrawing.Via via = pcb.vias().get(index);
            pieces.add(new Piece(Kind.VIA, index, key("via", index), "Via " + (index + 1), Optional.empty(), CopperShapes.via(via),
                    layer -> pcb.reaches(via, layer), nets.viaNets().get(index), nets.viaGroup(index)));
        }
        for (int index = 0; index < pcb.holes().size(); index++) {
            PcbDrawing.Hole hole = pcb.holes().get(index);
            pieces.add(new Piece(Kind.HOLE, index, key("hole", index), "Hole " + (index + 1), Optional.empty(),
                    CopperShape.circle(hole.x(), hole.y(), CopperShapes.reach(hole)), layer -> true, Optional.empty(), -1 - index));
        }
        for (Teardrops.Drop drop : drops) {
            if (drop.outline().size() < 3) {
                continue;
            }
            pieces.add(new Piece(Kind.DROP, drop.track(), key("track", drop.track()), "Track " + (drop.track() + 1) + "'s teardrop",
                    Optional.empty(), CopperShape.ring(drop.outline()), drop.layer()::equals, nets.trackNets().get(drop.track()),
                    nets.trackGroup(drop.track())));
        }
        return pieces;
    }

    /** What a finding or a choice names a thing of the pcb by: its kind and its place among them, {@code via:2}. */
    public static String key(String kind, int index) {
        return kind + ":" + index;
    }
}
