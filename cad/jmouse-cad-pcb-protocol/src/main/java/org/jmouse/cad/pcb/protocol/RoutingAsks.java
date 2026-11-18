package org.jmouse.cad.pcb.protocol;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.geometry.index.Box;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.routing.CopperNodes;
import org.jmouse.cad.pcb.routing.LengthTuning;
import org.jmouse.cad.pcb.routing.PartDrag;
import org.jmouse.cad.pcb.routing.PartDragMode;
import org.jmouse.cad.pcb.routing.RoutingEngine;
import org.jmouse.cad.pcb.routing.Tightening;
import org.jmouse.cad.pcb.routing.TrackRouter;
import org.jmouse.cad.pcb.routing.TuningPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * 🛤 What an editor asks the router of one pcb, and what it is answered: each gesture's request, its answer, and the call of the
 * {@link RoutingEngine} between them. The same wherever the engine runs — behind the server's routes, in its live session, in a
 * page — so the three answer alike. A gesture names no board: whoever holds the engine has already found it.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RoutingAsks {

    private RoutingAsks() {
    }

    // -------------------------------------------------------------------------------------------------------------- records

    public record StyleView(double angleStep, String jogs) {

        TrackRouter.LayingStyle style() {
            return new TrackRouter.LayingStyle(angleStep, Tightening.JogHandling.valueOf(jogs.toUpperCase(Locale.ROOT)));
        }
    }

    /** A track an answer leaves other than it was asked of, at its index among the pcb's tracks. */
    public record TrackChange(int index, Track track) {
    }

    /**
     * The tracks an answer leaves, said as what changed in the tracks it was asked of: as many tracks as there are now, and each one not
     * the same as the track at its index before. A gesture moves a few tracks of thousands, and the rest are neither sent back nor read
     * again, so an editor keeps every untouched track as the very object it held. Both none where the gesture cannot move at all.
     */
    public record TracksView(Integer count, List<TrackChange> changed) {

        /** No tracks: the gesture cannot move at all. */
        public static final TracksView NONE = new TracksView(null, null);

        /** What `after` changed in `before`. */
        public static TracksView between(List<Track> before, List<Track> after) {
            List<TrackChange> changed = new ArrayList<>();
            for (int index = 0; index < after.size(); index++) {
                Track now = after.get(index);
                if (index >= before.size() || !sameTrack(before.get(index), now)) {
                    changed.add(new TrackChange(index, now));
                }
            }
            return new TracksView(after.size(), changed);
        }

        private static boolean sameTrack(Track was, Track now) {
            return was == now || was.equals(now);
        }
    }

    public record LegRequest(PcbDrawing drawing, Integer own, String layer, String net, double width, Point from, Point to, double angleStep,
                             double rounding) {
    }

    public record LegView(List<Point> points, boolean reached, double rounding) {
    }

    public record SegmentRequest(PcbDrawing drawing, int track, int segment, double dx, double dy, StyleView style, Point pressed) {
    }

    public record CornerRequest(PcbDrawing drawing, int track, int corner, Point to, StyleView style) {
    }

    public record CarriedRequest(PcbDrawing before, PcbDrawing moved, double angleStep, String mode) {

        PartDrag drag() {
            return new PartDrag(before, moved, angleStep, PartDragMode.of(mode));
        }
    }

    public record JogsRequest(PcbDrawing drawing, List<Integer> chosen) {
    }

    /** Tracks whose corners were rounded, and where each chosen track now stands: the stretches of a line made one track. */
    public record CornersView(TracksView tracks, List<Integer> chosen) {
    }

    public record CornersRequest(PcbDrawing drawing, double rounding, List<Integer> chosen) {
    }

    public record NodesRequest(PcbDrawing drawing) {
    }

    public record TrackEndView(int track, String end) {
    }

    public record NodeView(String kind, Point at, double reach, Box box, List<String> layers, Integer item, List<TrackEndView> ends) {
    }

    public record NodesView(List<NodeView> nodes) {
    }

    public record TuneRequest(PcbDrawing drawing, String layer, Point point, double reach, Double length, String pattern) {
    }

    public record TuneTrackRequest(PcbDrawing drawing, int track, Double length, String pattern) {
    }

    public record TuningView(Integer track, List<Point> points, Double rounding, Double added, String net, Double from, Double to, String refusal) {

        static TuningView of(LengthTuning.Tuning tuning) {
            return switch (tuning) {
                case LengthTuning.Tuned tuned -> new TuningView(tuned.track(), tuned.points(), tuned.rounding(), tuned.added(), tuned.net().orElse(null),
                        tuned.from(), tuned.to(), null);
                case LengthTuning.Refused refused -> new TuningView(null, null, null, null, null, null, null, refused.refusal());
            };
        }
    }

    // ---------------------------------------------------------------------------------------------------------------- calls

    /** Where a track being laid goes, from its last corner toward the pointer or to a corner clicked. */
    public static LegView leg(RoutingEngine engine, LegRequest request) {
        RoutingEngine.LaidLeg laid = engine.leg(request.drawing(),
                request.own() == null ? OptionalInt.empty() : OptionalInt.of(request.own()), request.layer(), Optional.ofNullable(request.net()),
                request.width(), request.from(), request.to(), request.angleStep(), request.rounding());
        TrackRouter.RoutedLeg leg = laid.leg();
        return new LegView(leg.points(), leg.reached(), laid.rounding());
    }

    /** A track's segment carried; its tracks none where it cannot move at all. */
    public static TracksView segment(RoutingEngine engine, SegmentRequest request) {
        return engine.segmentCarried(request.drawing(), request.track(), request.segment(), request.dx(), request.dy(),
                        request.style().style(), Optional.ofNullable(request.pressed()))
                .map(carried -> TracksView.between(request.drawing().tracks(), carried.tracks()))
                .orElse(TracksView.NONE);
    }

    /** A track's corner carried to a point. */
    public static TracksView corner(RoutingEngine engine, CornerRequest request) {
        return TracksView.between(request.drawing().tracks(),
                engine.cornerCarried(request.drawing(), request.track(), request.corner(), request.to(), request.style().style()).tracks());
    }

    /** The tracks as moved parts go. */
    public static TracksView following(RoutingEngine engine, CarriedRequest request) {
        return TracksView.between(request.moved().tracks(), engine.tracksFollowing(request.drag()).tracks());
    }

    /** The tracks as moved parts are let go; refused where they cannot stand there. */
    public static TracksView laid(RoutingEngine engine, CarriedRequest request) {
        return switch (engine.tracksLaid(request.drag())) {
            case RoutingEngine.Release.Laid laid -> TracksView.between(request.moved().tracks(), laid.pcb().tracks());
            case RoutingEngine.Release.Refused refused -> throw new RoutingRefused(refused.reason());
        };
    }

    /** The tracks with their jogs merged, those chosen or every one. */
    public static TracksView jogs(RoutingEngine engine, JogsRequest request) {
        return TracksView.between(request.drawing().tracks(), engine.withoutJogs(request.drawing(), chosenOf(request.chosen())).tracks());
    }

    /** The tracks with their corners rounded, those chosen or every one; and where each chosen track now stands. */
    public static CornersView corners(RoutingEngine engine, CornersRequest request) {
        var rounded = engine.cornersRounded(request.drawing(), request.rounding(), chosenOf(request.chosen()));
        return new CornersView(TracksView.between(request.drawing().tracks(), rounded.pcb().tracks()), rounded.chosen());
    }

    /** Where the pcb's copper lines end and meet. */
    public static NodesView nodes(RoutingEngine engine, NodesRequest request) {
        CopperNodes nodes = engine.nodes(request.drawing());
        return new NodesView(nodes.all().stream().map(node -> new NodeView(node.kind().word(), node.at(), node.reach(), nodes.boxOf(node),
                node.layers().stream().sorted().toList(), node.item().isPresent() ? node.item().getAsInt() : null,
                node.ends().stream().map(end -> new TrackEndView(end.track(), end.end().name().toLowerCase(Locale.ROOT))).toList())).toList());
    }

    /** The track under a point bent to a length; or why not. */
    public static TuningView tune(RoutingEngine engine, TuneRequest request) {
        return TuningView.of(engine.tunedAt(request.drawing(), request.layer(), request.point(), request.reach(), lengthOf(request.length()),
                patternOf(request.pattern())));
    }

    /** A chosen track bent to a length wherever a meander fits best; or why not. */
    public static TuningView tuneTrack(RoutingEngine engine, TuneTrackRequest request) {
        return TuningView.of(engine.tunedAnywhere(request.drawing(), request.track(), lengthOf(request.length()), patternOf(request.pattern())));
    }

    private static List<Integer> chosenOf(List<Integer> chosen) {
        return chosen == null ? List.of() : chosen;
    }

    private static TuningPattern patternOf(String word) {
        return word == null || word.isBlank() ? TuningPattern.SQUARE : TuningPattern.valueOf(word.toUpperCase(Locale.ROOT));
    }

    private static OptionalDouble lengthOf(Double length) {
        return length == null ? OptionalDouble.empty() : OptionalDouble.of(length);
    }
}
