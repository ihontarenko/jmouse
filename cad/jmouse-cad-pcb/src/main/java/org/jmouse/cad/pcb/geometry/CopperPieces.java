package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Arc;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.Shape;
import org.jmouse.cad.pcb.drawing.Hole;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
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

    /** How a pour joins a piece of copper of its own net: across a thermal gap by spokes, so it can be soldered; or solid. */
    public enum PourJoining {
        THERMAL, SOLID
    }

    /** How a pour meets a piece of copper of no net: it stitches the pour, as a via of the pour's net would; or it is kept clear of. */
    public enum NetlessCopper {
        STITCHES, KEPT_CLEAR
    }

    /** What a piece of copper is, and how a pour meets it. */
    public enum Kind {
        PAD(PourJoining.THERMAL, NetlessCopper.KEPT_CLEAR),
        TRACK(PourJoining.SOLID, NetlessCopper.KEPT_CLEAR),
        ARC(PourJoining.SOLID, NetlessCopper.KEPT_CLEAR),
        DRAWN(PourJoining.SOLID, NetlessCopper.KEPT_CLEAR),
        VIA(PourJoining.SOLID, NetlessCopper.STITCHES),
        HOLE(PourJoining.SOLID, NetlessCopper.KEPT_CLEAR),
        DROP(PourJoining.SOLID, NetlessCopper.KEPT_CLEAR);

        private final PourJoining   joining;
        private final NetlessCopper netless;

        Kind(PourJoining joining, NetlessCopper netless) {
            this.joining = joining;
            this.netless = netless;
        }

        public PourJoining joining() {
            return joining;
        }

        public NetlessCopper netless() {
            return netless;
        }
    }

    /** A pad as it stands on the pcb: what a pad's copper is the copper of. */
    public record PadAt(FootprintDrawing.Pad pad, Placement placement) {
    }

    /** One outline of a shape drawn in copper: what a drawn piece is the copper of. */
    public record DrawnAt(Shape shape, int outline) {
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
     * @param origin  what in the document it is the copper of — a track, a via, a pad where it stands, an outline of a drawn shape —,
     *                compared by value: what a memory of copper knows it again by
     */
    public record Piece(Kind kind, int index, String key, String label, Optional<String> part, CopperShape shape,
                        Predicate<String> onLayer, Optional<String> net, int group, Object origin) {

        /** The same piece, named as on another net. */
        public Piece withNet(Optional<String> other) {
            return new Piece(kind, index, key, label, part, shape, onLayer, other, group, origin);
        }
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
                    Optional.of(pad.designator()), pad.copper(), pad::on, nets.padNets().get(index), nets.padGroup(index),
                    new PadAt(pad.pad(), pad.placement())));
        }
        for (int index = 0; index < pcb.tracks().size(); index++) {
            Track track = pcb.tracks().get(index);
            pieces.add(new Piece(Kind.TRACK, index, key("track", index), "Track " + (index + 1), Optional.empty(),
                    CopperShapes.track(track, curveSteps), track.layer()::equals, nets.trackNets().get(index), nets.trackGroup(index), track));
        }
        for (int index = 0; index < pcb.arcs().size(); index++) {
            Arc arc = pcb.arcs().get(index);
            pieces.add(new Piece(Kind.ARC, index, key("arc", index), "Arc " + (index + 1), Optional.empty(),
                    CopperShapes.arc(arc, curveSteps), arc.layer()::equals, nets.arcNets().get(index), nets.arcGroup(index), arc));
        }
        for (int index = 0; index < nets.drawn().size(); index++) {
            CopperShapes.Drawn drawn = nets.drawn().get(index);
            pieces.add(new Piece(Kind.DRAWN, index, key("shape", drawn.index()), "Shape " + (drawn.index() + 1), Optional.empty(),
                    drawn.shape(), drawn.layer()::equals, nets.drawnNets().get(index), nets.drawnGroup(index),
                    new DrawnAt(pcb.shapes().get(drawn.index()), drawn.outline())));
        }
        for (int index = 0; index < pcb.vias().size(); index++) {
            Via via = pcb.vias().get(index);
            pieces.add(new Piece(Kind.VIA, index, key("via", index), "Via " + (index + 1), Optional.empty(), CopperShapes.via(via),
                    layer -> pcb.reaches(via, layer), nets.viaNets().get(index), nets.viaGroup(index), via));
        }
        for (int index = 0; index < pcb.holes().size(); index++) {
            Hole hole = pcb.holes().get(index);
            pieces.add(new Piece(Kind.HOLE, index, key("hole", index), "Hole " + (index + 1), Optional.empty(),
                    CopperShape.circle(hole.x(), hole.y(), CopperShapes.reach(hole)), layer -> true, Optional.empty(), -1 - index, hole));
        }
        for (Teardrops.Drop drop : drops) {
            if (drop.outline().size() < 3) {
                continue;
            }
            pieces.add(new Piece(Kind.DROP, drop.track(), key("track", drop.track()), "Track " + (drop.track() + 1) + "'s teardrop",
                    Optional.empty(), CopperShape.ring(drop.outline()), drop.layer()::equals, nets.trackNets().get(drop.track()),
                    nets.trackGroup(drop.track()), drop));
        }
        return pieces;
    }

    /** What a finding or a choice names a thing of the pcb by: its kind and its place among them, {@code via:2}. */
    public static String key(String kind, int index) {
        return kind + ":" + index;
    }
}
