package org.jmouse.cad.pcb.geometry;

import org.jmouse.cad.pcb.drawing.PcbDrawing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The drops a pcb widens its tracks with where they enter copper, as KiCad lays teardrops: at each end of a track that
 * stands in a via, a pad, or the middle of another track on its layer, when the pcb asks for drops on that target and
 * the track is narrower than the drop would be. Each drop is a closed outline — its curved sides followed by short
 * straight steps —, copper of the track's net, drawn, checked and made as any other copper. The engine's one drop: the
 * editor draws what this makes, and the factory gets the same.
 *
 * <p>A drop is laid along the track, not along the copper's middle: a track seldom enters a pad through its centre. Its axis is
 * the track's own line, and each side of its base stands on the copper entered — as far out as the copper reaches on that side
 * and no farther —, whatever that copper's shape and however it is turned. So nothing of a drop stands out of the copper it
 * widens into, and a track entering near a pad's corner is widened only on the side the pad is: Ivan's board of 2026-11-06 had
 * bottom tracks entering pads near their corners, and each drop stuck out of the pad on both sides as a fishtail.</p>
 */
public final class Teardrops {

    /** The targets a pcb names its drops by. */
    private static final String ROUND   = "round";
    private static final String SURFACE = "surface";
    private static final String TRACKS  = "tracks";

    /**
     * How a drop is shaped, beyond what a pcb sets of it.
     *
     * @param curveSteps  how many straight steps follow a curved side of a drop
     * @param widestShare the widest a drop's base may be, as a share of what it enters: past that its base would stand out
     *                    of the copper as a shelf
     * @param insideEdge  how far the base stands from the middle of the copper on its line towards the copper's edge, as a
     *                    share of the way: inside the copper, so nothing of the drop stands out behind the track
     * @param halvings    how many halvings find where copper ends along a line, or how wide a drop's side may be
     * @param looks       how many steps a line beside the track is looked along for any of the copper
     */
    public record Shape(int curveSteps, double widestShare, double insideEdge, int halvings, int looks) {
    }

    /** A drop: the layer it is on, the track it widens, and its outline in millimetres. */
    public record Drop(String layer, int track, List<PcbDrawing.Point> outline) {
    }

    /** What a track's end stands in: the target's kind, its copper, and its size — what a drop's measures are shares of. */
    private record Entered(String target, CopperShape copper, double size) {
    }

    /** Where the copper lies on a line beside the track's axis: from how far back along the axis to how far on. */
    private record Chord(double from, double to) {
    }

    /** One side of a drop: which side, how far out its base reaches, and the copper on its line, if any. */
    private record Side(int sign, double half, Optional<Chord> chord) {
    }

    /** The track's end and the way it goes: what every point of a drop is measured from. */
    private record Frame(PcbDrawing.Point end, double[] along) {

        PcbDrawing.Point at(double distance, double lateral) {
            return new PcbDrawing.Point(end.x() + along[0] * distance - along[1] * lateral, end.y() + along[1] * distance + along[0] * lateral);
        }
    }

    private Teardrops() {
    }

    /**
     * Every drop the pcb's tracks are widened with.
     *
     * @param pads the pcb's pads where their parts stand them
     */
    public static List<Drop> of(PcbDrawing pcb, List<CopperNets.PlacedPad> pads, Shape shape) {
        if (pcb.teardrops().isEmpty()) {
            return List.of();
        }
        Map<String, PcbDrawing.Teardrop> settings = pcb.teardrops().stream()
                .collect(Collectors.toMap(PcbDrawing.Teardrop::target, Function.identity(), (first, second) -> first));
        List<Drop> drops = new ArrayList<>();
        for (int index = 0; index < pcb.tracks().size(); index++) {
            PcbDrawing.Track track = pcb.tracks().get(index);
            List<PcbDrawing.Point> points = track.points();
            if (points.size() < 2) {
                continue;
            }
            for (PcbDrawing.Point[] ends : List.of(new PcbDrawing.Point[] {points.getFirst(), points.get(1)},
                    new PcbDrawing.Point[] {points.getLast(), points.get(points.size() - 2)})) {
                PcbDrawing.Point end = ends[0];
                PcbDrawing.Point next = ends[1];
                double length = Math.hypot(next.x() - end.x(), next.y() - end.y());
                if (length == 0) {
                    continue;
                }
                Frame frame = new Frame(end, new double[] {(next.x() - end.x()) / length, (next.y() - end.y()) / length});
                int trackIndex = index;
                Optional<Entered> entered = viaAt(pcb, track, end).or(() -> padAt(pads, track, end)).or(() -> trackAt(pcb, trackIndex, track, end));
                Optional<PcbDrawing.Teardrop> setting = entered.map(Entered::target).map(settings::get);
                if (entered.isPresent() && setting.isPresent()) {
                    outline(entered.get(), setting.get(), frame, track.width(), length, shape)
                            .ifPresent(outline -> drops.add(new Drop(track.layer(), trackIndex, outline)));
                }
            }
        }
        return drops;
    }

    private static Optional<Entered> viaAt(PcbDrawing pcb, PcbDrawing.Track track, PcbDrawing.Point end) {
        return pcb.vias().stream()
                .filter(via -> pcb.reaches(via, track.layer()) && Math.hypot(via.x() - end.x(), via.y() - end.y()) <= via.diameter() / 2)
                .findFirst()
                .map(via -> new Entered(ROUND, CopperShape.circle(via.x(), via.y(), via.diameter() / 2), via.diameter()));
    }

    private static Optional<Entered> padAt(List<CopperNets.PlacedPad> pads, PcbDrawing.Track track, PcbDrawing.Point end) {
        for (CopperNets.PlacedPad pad : pads) {
            if (pad.on(track.layer()) && pad.copper().covers(end.x(), end.y())) {
                return Optional.of(new Entered(pad.pad().isSurface() ? SURFACE : ROUND, pad.copper(),
                        Math.min(pad.pad().width(), pad.pad().height())));
            }
        }
        return Optional.empty();
    }

    private static Optional<Entered> trackAt(PcbDrawing pcb, int index, PcbDrawing.Track track, PcbDrawing.Point end) {
        for (int otherIndex = 0; otherIndex < pcb.tracks().size(); otherIndex++) {
            PcbDrawing.Track other = pcb.tracks().get(otherIndex);
            if (otherIndex == index || !other.layer().equals(track.layer())) {
                continue;
            }
            for (int segment = 0; segment < other.points().size() - 1; segment++) {
                PcbDrawing.Point from = other.points().get(segment);
                PcbDrawing.Point to = other.points().get(segment + 1);
                double dx = to.x() - from.x();
                double dy = to.y() - from.y();
                double squared = dx * dx + dy * dy;
                double share = squared > 0 ? ((end.x() - from.x()) * dx + (end.y() - from.y()) * dy) / squared : 0;
                // A joint in the other track's middle: an end meeting its end carries it on, and is no T.
                if (share <= 0 || share >= 1) {
                    continue;
                }
                PcbDrawing.Point foot = new PcbDrawing.Point(from.x() + share * dx, from.y() + share * dy);
                if (Math.hypot(end.x() - foot.x(), end.y() - foot.y()) <= other.width() / 2) {
                    return Optional.of(new Entered(TRACKS, CopperShape.line(List.of(from, to), other.width() / 2), other.width()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Where the copper lies on the line {@code lateral} to one side of the track's axis: from how far back along the axis to
     * how far on, the axis measured from the track's end towards where the track goes. Looked for along the line from twice
     * {@code size} behind the end to twice {@code size} past it — copper is never larger —; empty where none of it is on that line.
     */
    private static Optional<Chord> chordOf(CopperShape copper, Frame frame, double lateral, double size, Shape shape) {
        DoublePredicate inside = distance -> {
            PcbDrawing.Point point = frame.at(distance, lateral);
            return copper.covers(point.x(), point.y());
        };
        double reach = 2 * size;
        Optional<Double> found = Optional.empty();
        for (int look = 0; look <= shape.looks(); look++) {
            double distance = -reach + look * 2 * reach / shape.looks();
            if (inside.test(distance)) {
                found = Optional.of(distance);
                break;
            }
        }
        if (found.isEmpty()) {
            return Optional.empty();
        }
        double on = found.get();
        return Optional.of(new Chord(edgeBetween(inside, on, on - 2 * reach, shape), edgeBetween(inside, on, on + 2 * reach, shape)));
    }

    /** Where the copper ends between a point on it and one off it, halved to it. */
    private static double edgeBetween(DoublePredicate inside, double on, double off, Shape shape) {
        double low = on;
        double high = off;
        for (int halving = 0; halving < shape.halvings(); halving++) {
            double middle = (low + high) / 2;
            if (inside.test(middle)) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /** How far to one side of the axis a drop's base can reach and still stand on the copper: no more than {@code widest}. */
    private static double sideReach(CopperShape copper, Frame frame, int sign, double widest, double size, Shape shape) {
        if (chordOf(copper, frame, sign * widest, size, shape).isPresent()) {
            return widest;
        }
        double low = 0;
        double high = widest;
        for (int halving = 0; halving < shape.halvings(); halving++) {
            double middle = (low + high) / 2;
            if (chordOf(copper, frame, sign * middle, size, shape).isPresent()) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /**
     * A drop's outline, along the track from the track's end: from a base on the copper entered — each side {@code width} of
     * its size out from the track's axis at most, and no farther than the copper goes on that side —, to the track's own width
     * at {@code length} of its size past where the track leaves the copper — no farther than the track's first segment
     * reaches —, its sides straight or curved in. Empty where the track is already as wide as the drop would be on both sides.
     */
    private static Optional<List<PcbDrawing.Point>> outline(Entered entered, PcbDrawing.Teardrop setting, Frame frame, double trackWidth,
                                                              double segmentLength, Shape shape) {
        double size = entered.size();
        double widest = Math.min(setting.width(), shape.widestShare()) * size / 2;
        double halfTrack = trackWidth / 2;
        if (widest <= halfTrack) {
            return Optional.empty();
        }
        Optional<Chord> axis = chordOf(entered.copper(), frame, 0, size, shape);
        if (axis.isEmpty()) {
            return Optional.empty();
        }
        // Where the track's own line leaves the copper: what the drop's length is measured past.
        double leaves = axis.get().to();
        double reach = Math.min(leaves + setting.length() * size, segmentLength);
        if (reach <= leaves) {
            return Optional.empty();
        }
        List<Side> sides = new ArrayList<>();
        for (int sign : new int[] {1, -1}) {
            double half = sideReach(entered.copper(), frame, sign, widest, size, shape);
            sides.add(new Side(sign, half, half > halfTrack ? chordOf(entered.copper(), frame, sign * half, size, shape) : Optional.empty()));
        }
        if (sides.stream().allMatch(side -> side.chord().isEmpty())) {
            return Optional.empty();
        }
        List<PcbDrawing.Point> outline = new ArrayList<>(side(sides.get(0), frame, leaves, reach, halfTrack, setting.curved(), shape));
        List<PcbDrawing.Point> other = new ArrayList<>(side(sides.get(1), frame, leaves, reach, halfTrack, setting.curved(), shape));
        Collections.reverse(other);
        outline.addAll(other);
        return Optional.of(outline);
    }

    /**
     * One side of a drop, from its base to the track's edge. With no copper beside the track on that side it runs along the
     * track's own edge; else it starts inside the copper and, curved in, leaves the copper square to the track and comes onto
     * the track's edge in line with it, joining both with no corner.
     */
    private static List<PcbDrawing.Point> side(Side side, Frame frame, double leaves, double reach, double halfTrack, boolean curved,
                                                 Shape shape) {
        PcbDrawing.Point to = frame.at(reach, side.sign() * halfTrack);
        if (side.chord().isEmpty()) {
            return List.of(frame.at(Math.min(leaves, reach), side.sign() * halfTrack), to);
        }
        Chord chord = side.chord().get();
        double middle = (chord.from() + chord.to()) / 2;
        double base = middle + (chord.to() - middle) * shape.insideEdge();
        PcbDrawing.Point from = frame.at(base, side.sign() * side.half());
        if (!curved) {
            return List.of(from, to);
        }
        PcbDrawing.Point bend = frame.at(Math.max(base, leaves), side.sign() * halfTrack);
        List<PcbDrawing.Point> points = new ArrayList<>();
        for (int step = 0; step <= shape.curveSteps(); step++) {
            double share = (double) step / shape.curveSteps();
            double rest = 1 - share;
            points.add(new PcbDrawing.Point(rest * rest * from.x() + 2 * rest * share * bend.x() + share * share * to.x(),
                    rest * rest * from.y() + 2 * rest * share * bend.y() + share * share * to.y()));
        }
        return points;
    }
}
