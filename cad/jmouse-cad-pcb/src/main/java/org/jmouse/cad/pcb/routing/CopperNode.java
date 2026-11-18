package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * A place copper lines end or meet at — a pad, a via, a junction, an arc's end, a track's free tip, a plain joint —: what it is,
 * where it stands, the layers it is on, and what meets it there. Every question of where a line ends and what holds it is asked
 * of these ({@link CopperNodes}), never worked out again by whoever needs it.
 *
 * @param at      where it stands: a pad's or a via's middle; else the first end met there, as the copper has it
 * @param reach   how far round it a point is at it: half a pad's smaller side, a via's radius, half the narrowest copper meeting
 * @param item    which pad (among the board's placed pads), via, or — for a tip — track it is; none for a junction, an arc's end
 *                or a joint
 * @param ends    the tracks' ends at it
 * @param arcs    the arcs ending at it
 * @param through the tracks it stands on away from their ends: what makes a line ending on one a T
 */
public record CopperNode(CopperNodeKind kind, Point at, double reach, Set<String> layers, OptionalInt item, List<TrackEnd> ends,
                         List<Integer> arcs, List<Integer> through) {

    public CopperNode {
        layers = Set.copyOf(layers);
        ends = List.copyOf(ends);
        arcs = List.copyOf(arcs);
        through = List.copyOf(through);
    }

    /** Which end of a track. */
    public enum End {
        FIRST, LAST
    }

    /** A track's end: the track, and which of its ends. */
    public record TrackEnd(int track, End end) {

        /** Where it stands on the pcb. */
        public Point on(PcbDrawing pcb) {
            List<Point> points = pcb.tracks().get(track).points();
            return end == End.FIRST ? points.getFirst() : points.getLast();
        }
    }

    /** Whether it is on a layer. */
    public boolean on(String layer) {
        return layers.contains(layer);
    }

    /** Whether a point stands at it: within its reach. */
    public boolean holds(Point point) {
        return at.distanceTo(point) <= reach;
    }

    /** The other track's end at a plain joint, from one of its two. */
    public TrackEnd partnerOf(int track) {
        return ends.getFirst().track() == track ? ends.getLast() : ends.getFirst();
    }
}
