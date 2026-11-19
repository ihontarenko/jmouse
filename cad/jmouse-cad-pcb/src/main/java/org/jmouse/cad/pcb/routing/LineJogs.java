package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Jogs merged along a line: a step too short to be meant, where the stretches either side can meet and keep their gaps. A jog is the
 * line's, not a track's: one standing at a joint — a track ending, the next beginning — is merged as any other, and each track takes
 * its own part of the line back ({@link JoinedTracks}). A line holding a track held where it stands is left as it is. No track is
 * taken away or added, so the chosen stay where they stood.
 *
 * @param worlds   the copper a pcb is known by: what a line keeps clear of
 * @param settings how short a step is too short
 */
record LineJogs(Function<PcbDrawing, RoutingWorld> worlds, RoutingSettings.Routing settings) implements LineMerging {

    /** The angle a merged line keeps to. */
    private static final double ANGLE_STEP = 45;

    @Override
    public Merged merged(PcbDrawing pcb, List<Integer> chosen) {
        RoutingWorld world = worlds.apply(pcb);
        PcbDrawing merged = pcb;
        Set<Integer> seen = new HashSet<>();
        for (int index = 0; index < pcb.tracks().size(); index++) {
            if (!chosen.isEmpty() && !chosen.contains(index) || seen.contains(index)) {
                continue;
            }
            JoinedTracks.JoinedLine joined = JoinedTracks.joinedLine(world, index);
            joined.members().forEach(member -> seen.add(member.index()));
            if (joined.members().stream().anyMatch(member -> RoutingEngine.held(pcb, member.index()))) {
                continue;
            }
            List<Point> line = joined.points();
            TrackFence fence = world.fenceOfTrack(pcb.tracks().get(index).withPoints(line), index, world.netOfTrack(index));
            List<Point> without = new Tightening(fence, ANGLE_STEP, List.of(), settings).withoutShortJogs(line, false);
            // Only a jog gone counts: a point standing on a straight run is a joint's, never a jog, and stays where it is.
            if (without.size() < LineCleaning.withoutStraightPoints(line).size()) {
                merged = JoinedTracks.withJoinedLine(merged, joined, without);
            }
        }
        return new Merged(merged, chosen);
    }
}
