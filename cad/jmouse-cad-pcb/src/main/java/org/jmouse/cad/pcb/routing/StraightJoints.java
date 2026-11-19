package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Two tracks of one kind — one layer, one width, one rounding, arranged and finished alike — meeting at a plain joint
 * where the line runs straight on: one conductor drawn as two, the joint a bead on it. Merged, they are one track, the joint gone.
 * A joint where the line turns is a corner of the line and stays a joint here; a held track is never merged.
 *
 * @param worlds the copper a pcb is known by: where its joints are
 */
record StraightJoints(Function<PcbDrawing, RoutingWorld> worlds) implements LineMerging {

    /** Two tracks one after the other along a line, and the points of both as one. */
    private record Straight(int kept, int gone, List<Point> points) {
    }

    @Override
    public Merged merged(PcbDrawing pcb, List<Integer> chosen) {
        PcbDrawing merged = pcb;
        List<Boolean> asked = new ArrayList<>(IntStream.range(0, pcb.tracks().size()).mapToObj(index -> chosen.isEmpty() || chosen.contains(index)).toList());
        for (int round = pcb.tracks().size(); round > 0; round--) {
            Optional<Straight> straight = straightJoint(merged, asked);
            if (straight.isEmpty()) {
                break;
            }
            List<Track> tracks = new ArrayList<>(merged.tracks());
            tracks.set(straight.get().kept(), tracks.get(straight.get().kept()).withPoints(straight.get().points()));
            tracks.remove(straight.get().gone());
            asked.set(straight.get().kept(), true);
            asked.remove(straight.get().gone());
            merged = merged.withTracks(List.copyOf(tracks));
        }
        List<Integer> standing = IntStream.range(0, asked.size()).filter(asked::get).boxed().toList();
        return new Merged(merged, chosen.isEmpty() ? List.of() : standing);
    }

    /** The first straight joint of a line holding a track asked about, its two tracks of one kind and neither held. */
    private Optional<Straight> straightJoint(PcbDrawing pcb, List<Boolean> asked) {
        RoutingWorld world = worlds.apply(pcb);
        for (int index = 0; index < pcb.tracks().size(); index++) {
            if (!asked.get(index)) {
                continue;
            }
            JoinedTracks.JoinedLine joined = JoinedTracks.joinedLine(world, index);
            List<Point> line = joined.points();
            for (int position = 0; position + 1 < joined.members().size(); position++) {
                int one = joined.members().get(position).index();
                int other = joined.members().get(position + 1).index();
                int joint = joined.joints().get(position);
                boolean straight = LineCleaning.withoutStraightPoints(List.of(line.get(joint - 1), line.get(joint), line.get(joint + 1))).size() == 2;
                if (!straight || !oneKind(pcb.tracks().get(one), pcb.tracks().get(other)) || RoutingEngine.held(pcb, one) || RoutingEngine.held(pcb, other)) {
                    continue;
                }
                int end = position + 2 < joined.members().size() ? joined.joints().get(position + 1) : line.size() - 1;
                List<Point> points = new ArrayList<>(line.subList(joined.starts().get(position), end + 1));
                points.remove(joint - joined.starts().get(position));
                return Optional.of(new Straight(Math.min(one, other), Math.max(one, other), List.copyOf(points)));
            }
        }
        return Optional.empty();
    }

    /** Whether two tracks are of one kind: on one layer, as wide, as rounded, arranged and finished alike. */
    private static boolean oneKind(Track one, Track other) {
        return one.layer().equals(other.layer()) && one.width() == other.width() && one.rounding() == other.rounding()
               && one.arrangement().equals(other.arrangement()) && one.finish().equals(other.finish());
    }
}
