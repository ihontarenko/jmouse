package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * What an editor asks of the router for one pcb: everything a person does to copper by hand, as one thing. A drag is asked of
 * the layout it began on, frame after frame, and what is worked out of that layout is kept for the whole of it. One request at
 * a time: the methods are the engine's, and a board's engine works one of them through before the next.
 */
public final class RoutingEngine {

    /** How many layouts of the board are kept worked out: a drag's start, and the frames it is asked of. */
    private static final int KEPT_WORLDS = 8;


    private final RoutingBoard                   board;
    private final Map<WorldKey, RoutingWorld>    worlds = new LinkedHashMap<>(KEPT_WORLDS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<WorldKey, RoutingWorld> eldest) {
            return size() > KEPT_WORLDS;
        }
    };
    /** The last board worked out whole: what a layout differing from it only in tracks laid again is worked out from. */
    private Optional<RoutingWorld>                lastWhole = Optional.empty();

    /** A world asked for: its layout, and the board a drag made it of, whose names its copper keeps; none, a layout of its own. */
    private record WorldKey(PcbDrawing layout, Optional<PcbDrawing> draggedFrom) {
    }

    /** A track's corner or segment dragged. */
    private final TrackReshaping                 reshaping;

    /** Parts dragged and what that does to copper. */
    private final PartDragging                   parts;

    public RoutingEngine(RoutingBoard board) {
        this.board = board;
        this.reshaping = new TrackReshaping(this);
        this.parts = new PartDragging(this, board.settings());
    }

    public RoutingBoard board() {
        return board;
    }

    private RoutingSettings settings() {
        return board.settings();
    }

    /**
     * The board with this layout on it, worked out once — and where it is a layout the last board worked out whole differs from only in
     * some tracks laid again, worked out from that one: a track carried leaves the rest of the pcb as it was, and so does its world.
     */
    public synchronized RoutingWorld worldOf(PcbDrawing pcb) {
        return worlds.computeIfAbsent(new WorldKey(pcb, Optional.empty()), key -> {
            Optional<RoutingWorld> relaid = lastWhole.flatMap(whole -> whole.relaidAs(pcb));
            if (relaid.isPresent()) {
                return relaid.get();
            }
            RoutingWorld whole = new RoutingWorld(pcb, board);
            lastWhole = Optional.of(whole);
            return whole;
        });
    }

    /** The board with a layout a drag made on it, worked out once: its copper keeps the nets the board it began on named it by. */
    synchronized RoutingWorld draggedWorldOf(PcbDrawing layout, PartDrag drag) {
        if (layout.equals(drag.before())) {
            return worldOf(layout);
        }
        RoutingWorld before = worldOf(drag.before());
        return worlds.computeIfAbsent(new WorldKey(layout, Optional.of(drag.before())), key -> RoutingWorld.draggedFrom(before, layout));
    }

    /**
     * A leg as it is laid: the way it goes, and the corners it is drawn with — those the reader asked for as far as the leg's
     * gaps let them, for a corner rounded is copper nearer what stands inside it than the sharp one the way was found by.
     */
    public record LaidLeg(TrackRouter.RoutedLeg leg, double rounding) {
    }

    /**
     * Where a track being laid goes, from its last corner toward the pointer, with the corners it may be drawn with.
     *
     * @param own the track of the pcb it carries on, if it carries one on: that track is not in its way, whatever its net — a track
     *            begun on bare pcb is on none, and would otherwise be another net's copper to its own next leg
     */
    public synchronized LaidLeg leg(PcbDrawing pcb, OptionalInt own, String layer, Optional<String> net, double width, Point from, Point to,
                                    double angleStep, double rounding) {
        RoutingWorld laidOn = worldOf(pcb);
        TrackRouter.RoutedLeg leg = TrackRouter.routedLeg(TrackRouter.LegRequest.of(laidOn, own, layer, net, width, from, to, angleStep));
        List<Point> found = Lines.joined(List.of(from), leg.points());
        TrackFence fence = laidOn.fence(layer, net, width, own).leaving(List.of(from, found.getLast()));
        RoutingSettings.Pads pads = settings().pads();
        List<Point> line = pads.exitKind() == PadExit.SMART ? PadBreakouts.atPads(laidOn, layer, found, fence, angleStep, pads) : found;
        double fits = fence.roundingThatFits(line, rounding, layer);
        return new LaidLeg(new TrackRouter.RoutedLeg(line.subList(1, line.size()), leg.reached()), fits);
    }

    /** The pcb with a track's segment carried by (dx, dy), its neighbours following; empty where it cannot move at all. */
    public synchronized Optional<PcbDrawing> segmentCarried(PcbDrawing pcb, int index, int own, double dx, double dy,
                                                            TrackRouter.LayingStyle style, Optional<Point> pressed) {
        return reshaping.segmentCarried(pcb, index, own, dx, dy, style, pressed);
    }

    /** The pcb with a track's corner carried to a point, the runs either side of it kept. */
    public synchronized PcbDrawing cornerCarried(PcbDrawing pcb, int index, int own, Point to, TrackRouter.LayingStyle style) {
        return reshaping.cornerCarried(pcb, index, own, to, style);
    }

    /** Where the pcb's copper lines end and meet: its pads, vias, junctions, arcs' ends, free tips and joints. */
    public synchronized CopperNodes nodes(PcbDrawing pcb) {
        return worldOf(pcb).nodes();
    }

    /** The pcb's tracks with their jogs merged, those chosen or every one where none is, each only where the corner left keeps its gaps. */
    public synchronized PcbDrawing withoutJogs(PcbDrawing pcb, List<Integer> chosen) {
        RoutingWorld world = worldOf(pcb);
        List<PcbDrawing.Track> tracks = new ArrayList<>(pcb.tracks());
        for (int index = 0; index < tracks.size(); index++) {
            if (!chosen.isEmpty() && !chosen.contains(index) || held(pcb, index)) {
                continue;
            }
            PcbDrawing.Track track = tracks.get(index);
            List<Point> line = track.points();
            TrackFence fence = world.fenceOfTrack(track, index, world.netOfTrack(index));
            List<Point> merged = new Tightening(fence, 45, List.of(), settings().routing()).withoutShortJogs(line, false);
            if (merged.size() < line.size()) {
                tracks.set(index, track.withPoints(merged));
            }
        }
        return pcb.withTracks(tracks);
    }

    /** The pcb's tracks with their corners rounded so, those chosen or every one where none is, each only as far as its gaps let it. */
    public synchronized PcbDrawing withCorners(PcbDrawing pcb, double rounding, List<Integer> chosen) {
        RoutingWorld world = worldOf(pcb);
        List<PcbDrawing.Track> tracks = new ArrayList<>(pcb.tracks());
        for (int index = 0; index < tracks.size(); index++) {
            if (!chosen.isEmpty() && !chosen.contains(index) || held(pcb, index)) {
                continue;
            }
            PcbDrawing.Track track = tracks.get(index);
            TrackFence fence = world.fenceOfTrack(track, index, world.netOfTrack(index));
            tracks.set(index, track.withRounding(fence.roundingThatFits(track.points(), rounding, track.layer())));
        }
        return pcb.withTracks(tracks);
    }

    /** The track under a point on a layer bent to a length, the meander where it was pointed at; or why it cannot be. */
    public synchronized LengthTuning.Tuning tunedAt(PcbDrawing pcb, String layer, Point point, double reach, OptionalDouble length, TuningPattern pattern) {
        return new LengthTuning(worldOf(pcb)).tunedAt(layer, point, reach, length, pattern);
    }

    /** A chosen track bent to a length wherever on it a meander fits best; or why it cannot be. */
    public synchronized LengthTuning.Tuning tunedAnywhere(PcbDrawing pcb, int track, OptionalDouble length, TuningPattern pattern) {
        return new LengthTuning(worldOf(pcb)).tunedAnywhere(track, length, pattern);
    }

    /**
     * The pcb as moved parts go: they and the copper standing wholly on them; detached, the lines as they ran, reconnected, each line
     * they pull stretched straight to its pad — nothing laid round what it meets as the hand moves. The editor says only what it moved;
     * what its copper does is worked out here ({@link PartDragging}).
     */
    public synchronized PcbDrawing tracksFollowing(PartDrag drag) {
        return parts.following(drag);
    }

    /** The lines a drag pulls or pushes as a release would lay them as rubber bands, before any that could not be is cut back. */
    synchronized PcbDrawing tracksBanded(PartDrag drag) {
        return parts.banded(drag);
    }

    /** The same when the parts are let go — moved, turned or put on the other side —: what is kept, or that they cannot stand there. */
    public synchronized Release tracksLaid(PartDrag drag) {
        return parts.released(drag);
    }

    /**
     * What a release answers: the pcb as laid, or that what was moved cannot stand where it was let go — its own copper would meet
     * copper of another net. Copper of two nets never meets because of a move: such a move is not taken; a line that cannot be laid
     * again is cut back, not a reason to refuse.
     */
    public sealed interface Release {

        /** The pcb with the lines the move pulled or pushed laid again. */
        record Laid(PcbDrawing pcb) implements Release {
        }

        /** Where copper of two nets would meet — as {@code B pad 1 of N would reach C pad 1 of M through Via 1, Track 1}. */
        record Refused(List<String> meetings, int named) implements Release {

            /** Why, in words: the first few places named, as the checks name a short's, the rest counted. */
            public String reason() {
                List<String> first = meetings.subList(0, Math.min(named, meetings.size()));
                int rest = meetings.size() - first.size();
                return "It cannot stand there: " + String.join("; ", first) + (rest > 0 ? "; and " + rest + " more" : "")
                       + ". Nothing there could be laid round it.";
            }
        }
    }

    /** Whether a track is held where it stands — locked itself or in a locked group —: no act of the router moves it. */
    static boolean held(PcbDrawing pcb, int track) {
        return pcb.drawnGroups().holds(pcb.tracks().get(track).arrangement());
    }
}
