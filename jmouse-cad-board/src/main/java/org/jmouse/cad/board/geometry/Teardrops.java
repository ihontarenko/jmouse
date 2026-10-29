package org.jmouse.cad.board.geometry;

import org.jmouse.cad.board.drawing.BoardDrawing;
import org.locationtech.jts.geom.Envelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The drops a board widens its tracks with where they enter copper, as KiCad lays teardrops: at each end of a track that
 * stands in a via, a pad, or the middle of another track on its layer, when the board asks for drops on that target and
 * the track is narrower than the drop would be. Each drop is a closed outline, copper of the track's net.
 */
public final class Teardrops {

    /** The targets a board names its drops by. */
    private static final String ROUND   = "round";
    private static final String SURFACE = "surface";
    private static final String TRACKS  = "tracks";

    /** A drop: the layer it is on, the track it widens, and its outline in millimetres. */
    public record Drop(String layer, int track, List<BoardDrawing.Point> outline) {
    }

    /** What a track's end stands in: the target's kind, the copper's middle, its size, and how far its edge is along the track. */
    private record Entered(String target, BoardDrawing.Point centre, double size, double edge) {
    }

    /** Where a drop stands: the copper entered, the way the track leaves it, and the way across. */
    private record Frame(Entered entered, double[] along, double[] across) {

        BoardDrawing.Point at(double distance, double side) {
            return new BoardDrawing.Point(entered.centre().x() + along[0] * distance + across[0] * side,
                    entered.centre().y() + along[1] * distance + across[1] * side);
        }
    }

    private Teardrops() {
    }

    /**
     * @param pads       the board's pads where their parts stand them
     * @param curveSteps how many straight steps follow a curved side of a drop
     */
    public static List<Drop> of(BoardDrawing board, List<CopperNets.PlacedPad> pads, int curveSteps) {
        if (board.teardrops().isEmpty()) {
            return List.of();
        }
        Map<String, BoardDrawing.Teardrop> settings = board.teardrops().stream()
                .collect(Collectors.toMap(BoardDrawing.Teardrop::target, Function.identity(), (first, second) -> first));
        List<Drop> drops = new ArrayList<>();
        for (int index = 0; index < board.tracks().size(); index++) {
            BoardDrawing.Track track = board.tracks().get(index);
            List<BoardDrawing.Point> points = track.points();
            if (points.size() < 2) {
                continue;
            }
            for (BoardDrawing.Point[] ends : List.of(new BoardDrawing.Point[] {points.getFirst(), points.get(1)},
                    new BoardDrawing.Point[] {points.getLast(), points.get(points.size() - 2)})) {
                BoardDrawing.Point end = ends[0];
                BoardDrawing.Point next = ends[1];
                double length = Math.hypot(next.x() - end.x(), next.y() - end.y());
                if (length == 0) {
                    continue;
                }
                double[] along = {(next.x() - end.x()) / length, (next.y() - end.y()) / length};
                int trackIndex = index;
                Optional<Entered> entered = viaAt(board, track, end)
                        .or(() -> padAt(pads, track, end, along))
                        .or(() -> trackAt(board, trackIndex, track, end));
                Optional<BoardDrawing.Teardrop> setting = entered.map(Entered::target).map(settings::get);
                if (entered.isPresent() && setting.isPresent()) {
                    outline(entered.get(), setting.get(), along, track.width(), length, curveSteps)
                            .ifPresent(outline -> drops.add(new Drop(track.layer(), trackIndex, outline)));
                }
            }
        }
        return drops;
    }

    private static Optional<Entered> viaAt(BoardDrawing board, BoardDrawing.Track track, BoardDrawing.Point end) {
        return board.vias().stream()
                .filter(via -> board.reaches(via, track.layer()) && Math.hypot(via.x() - end.x(), via.y() - end.y()) <= via.diameter() / 2)
                .findFirst()
                .map(via -> new Entered(ROUND, new BoardDrawing.Point(via.x(), via.y()), via.diameter(), via.diameter() / 2));
    }

    private static Optional<Entered> padAt(List<CopperNets.PlacedPad> pads, BoardDrawing.Track track, BoardDrawing.Point end,
                                           double[] along) {
        for (CopperNets.PlacedPad pad : pads) {
            Envelope bounds = pad.shape().getEnvelopeInternal();
            if (!pad.pad().hasCopperOn(track.layer(), pad.placement()) || !bounds.contains(end.x(), end.y())) {
                continue;
            }
            double size = Math.min(bounds.getWidth(), bounds.getHeight());
            // How far the pad's side is from its middle the way the track leaves: a circle's radius, a rectangle's nearer side.
            boolean round = "circle".equals(pad.pad().shape()) || "oval".equals(pad.pad().shape()) && bounds.getWidth() == bounds.getHeight();
            double edge = round ? size / 2 : Math.min(
                    Math.abs(along[0]) > 1e-9 ? bounds.getWidth() / 2 / Math.abs(along[0]) : Double.POSITIVE_INFINITY,
                    Math.abs(along[1]) > 1e-9 ? bounds.getHeight() / 2 / Math.abs(along[1]) : Double.POSITIVE_INFINITY);
            return Optional.of(new Entered(pad.pad().isSurface() ? SURFACE : ROUND, pad.centre(), size, edge));
        }
        return Optional.empty();
    }

    private static Optional<Entered> trackAt(BoardDrawing board, int index, BoardDrawing.Track track, BoardDrawing.Point end) {
        for (int otherIndex = 0; otherIndex < board.tracks().size(); otherIndex++) {
            BoardDrawing.Track other = board.tracks().get(otherIndex);
            if (otherIndex == index || !other.layer().equals(track.layer())) {
                continue;
            }
            for (int segment = 0; segment < other.points().size() - 1; segment++) {
                BoardDrawing.Point from = other.points().get(segment);
                BoardDrawing.Point to = other.points().get(segment + 1);
                double dx = to.x() - from.x();
                double dy = to.y() - from.y();
                double squared = dx * dx + dy * dy;
                double share = squared > 0 ? ((end.x() - from.x()) * dx + (end.y() - from.y()) * dy) / squared : 0;
                // A joint in the other track's middle: an end meeting its end carries it on, and is no T.
                if (share <= 0 || share >= 1) {
                    continue;
                }
                BoardDrawing.Point foot = new BoardDrawing.Point(from.x() + share * dx, from.y() + share * dy);
                if (Math.hypot(end.x() - foot.x(), end.y() - foot.y()) <= other.width() / 2) {
                    return Optional.of(new Entered(TRACKS, foot, other.width(), other.width() / 2));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * A drop's outline: from a base inside the copper entered, {@code width} of its size across, to the track's own width
     * at {@code length} of its size past its edge — no farther than the track's first segment reaches —, its sides
     * straight or curved in. Empty where the track is already as wide as the drop would be.
     */
    private static Optional<List<BoardDrawing.Point>> outline(Entered entered, BoardDrawing.Teardrop setting, double[] along,
                                                              double trackWidth, double segmentLength, int curveSteps) {
        double halfBase = setting.width() * entered.size() / 2;
        double halfTrack = trackWidth / 2;
        double reach = Math.min(entered.edge() + setting.length() * entered.size(), segmentLength);
        if (halfBase <= halfTrack || reach <= entered.edge()) {
            return Optional.empty();
        }
        Frame frame = new Frame(entered, along, new double[] {-along[1], along[0]});
        List<BoardDrawing.Point> outline = new ArrayList<>(side(frame, reach, halfBase, halfTrack, 1, setting.curved(), curveSteps));
        List<BoardDrawing.Point> other = new ArrayList<>(side(frame, reach, halfBase, halfTrack, -1, setting.curved(), curveSteps));
        Collections.reverse(other);
        outline.addAll(other);
        return Optional.of(outline);
    }

    /** One side of a drop: straight, or bent toward the track's edge where the copper entered ends, as a drop narrows to its tail. */
    private static List<BoardDrawing.Point> side(Frame frame, double reach, double halfBase, double halfTrack, int sign,
                                                 boolean curved, int curveSteps) {
        // The base stands inside the copper entered, half way to its edge.
        BoardDrawing.Point from = frame.at(frame.entered().edge() / 2, sign * halfBase);
        BoardDrawing.Point to = frame.at(reach, sign * halfTrack);
        if (!curved) {
            return List.of(from, to);
        }
        BoardDrawing.Point bend = frame.at(frame.entered().edge(), sign * halfTrack);
        List<BoardDrawing.Point> points = new ArrayList<>();
        for (int step = 0; step <= curveSteps; step++) {
            double share = (double) step / curveSteps;
            double rest = 1 - share;
            points.add(new BoardDrawing.Point(rest * rest * from.x() + 2 * rest * share * bend.x() + share * share * to.x(),
                    rest * rest * from.y() + 2 * rest * share * bend.y() + share * share * to.y()));
        }
        return points;
    }
}
