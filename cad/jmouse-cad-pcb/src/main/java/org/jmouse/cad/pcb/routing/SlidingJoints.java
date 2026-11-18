package org.jmouse.cad.pcb.routing;

import org.jmouse.numerics.Halving;
import java.util.OptionalDouble;
import java.util.function.DoubleFunction;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The joints of a drag's bands that do not hold them where they stand ({@link RoutingSettings.Banding#heldNodes}) — a third track
 * meeting a line, a via — slid to where the lines meeting at them are shortest together, as they are laid: the point whose laid
 * lengths to each line's next corner add up least ({@link FermatPoints}). A joint goes only as far as the way there is clear of other nets' copper —
 * halved until it is —, so nothing passes through it; its lines are pulled tight after it, round after round, until it rests.
 */
final class SlidingJoints {

    /** The pcb with its vias where they slid to, and the bands as they run after it. */
    record Slid(PcbDrawing pcb, List<Band> bands) {
    }

    /** A joint that slides: where it stands, which kind it is, the layers and net its lines are on, how wide it keeps clear. */
    private record Joint(Point at, CopperNodeKind kind, Optional<Integer> via, Set<String> layers, Optional<String> net, double width) {
    }

    /** How many halvings a joint's way is shortened by before it stays where it is. */

    /** How near a joint's place a joint is taken to rest at: the precision a point is kept to. */
    private static final double RESTED = Lines.SAME_POINT;

    private final RoutingWorld            standing;
    private final BandPulling             pulling;
    private final RoutingSettings.Banding settings;
    private final double                  angleStep;

    /** @param angleStep the angle the reader lays at: what a line's length is measured by */
    SlidingJoints(RoutingWorld standing, BandPulling pulling, double angleStep) {
        this.standing = standing;
        this.pulling = pulling;
        this.settings = standing.settings().banding();
        this.angleStep = angleStep;
    }

    Slid slid(List<Band> pulled) {
        if (settings.holds(CopperNodeKind.JUNCTION) && settings.holds(CopperNodeKind.VIA)) {
            return new Slid(standing.pcb(), pulled);
        }
        RoutingWorld world = standing;
        List<Band> bands = new ArrayList<>(pulled);
        for (int round = 0; round < settings.nodeRounds(); round++) {
            Map<Joint, Point> moves = new LinkedHashMap<>();
            for (Joint joint : jointsOf(world, bands)) {
                bands = withBandsMeeting(world, bands, joint);
                Point target = clearTowards(world, joint, FermatPoints.of(joint.at(), neighboursOf(bands, joint), angleStep));
                if (Lines.lengthBetween(target, joint.at()) > RESTED) {
                    moves.put(joint, target);
                    if (joint.via().isPresent()) {
                        world = RoutingWorld.draggedFrom(world, world.pcb().withVias(viasWith(world.pcb(), joint.via().get(), target)));
                    }
                }
            }
            if (moves.isEmpty()) {
                break;
            }
            List<Band> sliding = bands.stream().map(band -> slidingEnds(band, moves)).toList();
            double farthest = moves.entrySet().stream().mapToDouble(move -> Lines.lengthBetween(move.getKey().at(), move.getValue())).max().orElse(0);
            DragMotion still = DragMotion.still();
            bands = pulling.pulled(sliding, new BandField(world, still, sliding, Map.of()), still, farthest).stream().map(SlidingJoints::rested).toList();
        }
        return new Slid(world.pcb(), bands);
    }

    /** The joints the bands' held ends stand at that slide: not held by the settings, nor by anything that cannot follow. */
    private List<Joint> jointsOf(RoutingWorld world, List<Band> bands) {
        Set<Point> seen = new HashSet<>();
        List<Joint> joints = new ArrayList<>();
        for (Band band : bands) {
            for (BandEnd end : List.of(band.start(), band.end())) {
                if (end instanceof BandEnd.Held held && seen.add(held.was())) {
                    jointAt(world, held.was(), band).ifPresent(joints::add);
                }
            }
        }
        return joints;
    }

    /**
     * The joint a band's held end stands at, where it slides: a via or a junction ({@link CopperNodes}) the settings let go of, only
     * lines' ends meeting it — no arc, no track running on through it, nothing held where it stands —, the via free to move.
     */
    private Optional<Joint> jointAt(RoutingWorld world, Point point, Band band) {
        PcbDrawing pcb = world.pcb();
        Optional<CopperNode> found = world.nodes().at(band.layer(), point);
        if (found.isEmpty() || settings.holds(found.get().kind())) {
            return Optional.empty();
        }
        CopperNode node = found.get();
        boolean onlyLineEnds = node.arcs().isEmpty() && node.ends().stream().noneMatch(end -> RoutingEngine.held(pcb, end.track()));
        return switch (node.kind()) {
            case VIA -> {
                PcbDrawing.Via via = pcb.vias().get(node.item().getAsInt());
                boolean free = onlyLineEnds && !pcb.drawnGroups().holds(via.arrangement());
                yield free ? Optional.of(new Joint(node.at(), CopperNodeKind.VIA, Optional.of(node.item().getAsInt()), node.layers(), band.net(),
                        via.diameter())) : Optional.empty();
            }
            case JUNCTION -> onlyLineEnds && node.through().isEmpty()
                    ? Optional.of(new Joint(point, CopperNodeKind.JUNCTION, Optional.empty(), Set.of(band.layer()), band.net(), band.width()))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    /** Whether a point stands at a joint: within its copper. */
    private static boolean at(Joint joint, Point point) {
        return Lines.lengthBetween(joint.at(), point) <= joint.width() / 2;
    }

    /** The bands with every line ending at a joint made one, held at its other end, where it is not one yet. */
    private static List<Band> withBandsMeeting(RoutingWorld world, List<Band> bands, Joint joint) {
        Set<Integer> banded = new HashSet<>();
        bands.forEach(band -> banded.addAll(band.tracks()));
        List<Band> more = new ArrayList<>(bands);
        PcbDrawing pcb = world.pcb();
        for (int index = 0; index < pcb.tracks().size(); index++) {
            PcbDrawing.Track track = pcb.tracks().get(index);
            boolean endsHere = joint.layers().contains(track.layer()) && (at(joint, track.points().getFirst()) || at(joint, track.points().getLast()));
            if (!endsHere || banded.contains(index)) {
                continue;
            }
            JoinedTracks.JoinedLine line = JoinedTracks.joinedLine(world, index);
            line.members().forEach(member -> banded.add(member.index()));
            double width = line.members().stream().mapToDouble(member -> pcb.tracks().get(member.index()).width()).max().orElse(track.width());
            more.add(Band.whole(line, track.layer(), width, world.netOfTrack(index), new BandEnd.Held(line.points().getFirst()),
                    new BandEnd.Held(line.points().getLast())));
        }
        return more;
    }

    /** The next corner of each band from where it meets a joint. */
    private static List<Point> neighboursOf(List<Band> bands, Joint joint) {
        List<Point> neighbours = new ArrayList<>();
        for (Band band : bands) {
            List<Point> path = band.path();
            if (path.size() < 2) {
                continue;
            }
            if (at(joint, path.getFirst())) {
                neighbours.add(path.get(1));
            } else if (at(joint, path.getLast())) {
                neighbours.add(path.get(path.size() - 2));
            }
        }
        return neighbours;
    }

    /** As far towards a target as the way there is clear of other nets' copper on every layer the joint is on — halved until it is. */
    private static Point clearTowards(RoutingWorld world, Joint joint, Point target) {
        DoubleFunction<Point> at = share -> Lines.settled(joint.at().toward(target, share));
        OptionalDouble share = Halving.firstHeldShare(world.settings().banding().jointHalvings(), tried -> joint.layers().stream()
                .allMatch(layer -> world.fence(layer, joint.net(), joint.width()).leaving(List.of(joint.at())).lineClear(List.of(joint.at(), at.apply(tried)))));
        return share.isPresent() ? at.apply(share.getAsDouble()) : joint.at();
    }

    private static List<PcbDrawing.Via> viasWith(PcbDrawing pcb, int index, Point at) {
        List<PcbDrawing.Via> vias = new ArrayList<>(pcb.vias());
        vias.set(index, vias.get(index).at(at.x(), at.y()));
        return vias;
    }

    /** A band whose ends at moving joints slide with them. */
    private static Band slidingEnds(Band band, Map<Joint, Point> moves) {
        BandEnd start = slidingEnd(band.start(), moves);
        BandEnd end = slidingEnd(band.end(), moves);
        return start == band.start() && end == band.end() ? band
                : band.withEnds(start, end);
    }

    /** An end at a joint that moves goes as far the same way. */
    private static BandEnd slidingEnd(BandEnd end, Map<Joint, Point> moves) {
        if (end instanceof BandEnd.Held held) {
            for (Map.Entry<Joint, Point> move : moves.entrySet()) {
                Joint joint = move.getKey();
                if (at(joint, held.was())) {
                    Point to = Lines.settled(new Point(held.was().x() + move.getValue().x() - joint.at().x(),
                                                       held.was().y() + move.getValue().y() - joint.at().y()));
                    return new BandEnd.Sliding(held.was(), to);
                }
            }
        }
        return end;
    }

    /** A band whose sliding ends have come to rest where they slid to. */
    private static Band rested(Band band) {
        return band.withEnds(restedEnd(band.start()), restedEnd(band.end()));
    }

    private static BandEnd restedEnd(BandEnd end) {
        return end instanceof BandEnd.Sliding sliding ? new BandEnd.Held(sliding.to()) : end;
    }
}
