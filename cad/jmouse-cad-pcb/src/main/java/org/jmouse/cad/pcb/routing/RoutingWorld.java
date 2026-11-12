package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.document.FindingSeverity;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.checks.PcbChecks;
import org.jmouse.cad.pcb.checks.PcbFinding;
import org.jmouse.cad.pcb.drawing.NetRules;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.cad.pcb.geometry.Teardrops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

/**
 * A pcb as the router sees it: one layout of a board, its nets, its copper and its fences worked out once and kept. Everything
 * the router asks of the board it asks here, so laying a track, dragging one, shoving one and routing a whole pcb all measure
 * one way, against the same rules.
 *
 * <p>A world is asked by one router at a time.</p>
 */
public final class RoutingWorld {

    private final PcbDrawing                 pcb;
    private final RoutingBoard               board;
    private final NetRules                   rules;
    private final Map<FenceKey, TrackFence>  fences = new HashMap<>();
    private CopperNets                       nets;
    private List<CopperPieces.Piece>         pieces;
    private List<String>                     faults;

    /** A track asked about: what it is and what is left out of what it meets. */
    private record FenceKey(String layer, Optional<String> net, double width, OptionalInt except, boolean tracks) {
    }

    public RoutingWorld(PcbDrawing pcb, RoutingBoard board) {
        this.pcb = pcb;
        this.board = board;
        this.rules = new NetRules(pcb, board.values());
    }

    /** The same board with another layout on it. */
    public RoutingWorld of(PcbDrawing other) {
        return other.equals(pcb) ? this : new RoutingWorld(other, board);
    }

    public PcbDrawing pcb() {
        return pcb;
    }

    public RoutingBoard board() {
        return board;
    }

    public RoutingSettings settings() {
        return board.settings();
    }

    public NetRules rules() {
        return rules;
    }

    private int curveSteps() {
        return board.checks().curveSteps();
    }

    /** Which copper is on which net. */
    public CopperNets nets() {
        if (nets == null) {
            nets = CopperNets.of(pcb, board.footprintsOn(pcb), board.partNetsOn(pcb), board.outlines(), curveSteps());
        }
        return nets;
    }

    /** Every piece of copper the checks measure, a track's drops among its own. */
    public List<CopperPieces.Piece> pieces() {
        if (pieces == null) {
            pieces = CopperPieces.of(pcb, nets(), Teardrops.of(pcb, nets().pads(), board.checks().teardrops()), curveSteps());
        }
        return pieces;
    }

    public double clearanceOf(Optional<String> net) {
        return rules.clearanceOf(net);
    }

    public double trackWidthOf(Optional<String> net) {
        return rules.trackWidthOf(net);
    }

    public Optional<String> netOfTrack(int index) {
        return nets().trackNets().get(index);
    }

    /**
     * The net the copper under a point is on, on a layer: a pad's, as the circuit wires it; else a track's, an arc's or a via's,
     * as the pads it reaches are. What a new track started there joins. A zone is not asked: copper of any net may cross one,
     * which clears round it.
     */
    public Optional<String> netAt(String layer, Point point) {
        List<CopperNets.PlacedPad> pads = nets().pads();
        for (int index = 0; index < pads.size(); index++) {
            CopperNets.PlacedPad pad = pads.get(index);
            if (pad.on(layer) && pad.copper().covers(point.x(), point.y())) {
                return nets().padNets().get(index);
            }
        }
        for (int index = 0; index < pcb.tracks().size(); index++) {
            PcbDrawing.Track track = pcb.tracks().get(index);
            if (track.layer().equals(layer) && CopperShapes.track(track, curveSteps()).covers(point.x(), point.y())) {
                return nets().trackNets().get(index);
            }
        }
        for (int index = 0; index < pcb.arcs().size(); index++) {
            PcbDrawing.Arc arc = pcb.arcs().get(index);
            if (arc.layer().equals(layer) && CopperShapes.arc(arc, curveSteps()).covers(point.x(), point.y())) {
                return nets().arcNets().get(index);
            }
        }
        for (int index = 0; index < pcb.vias().size(); index++) {
            PcbDrawing.Via via = pcb.vias().get(index);
            if (Math.hypot(via.x() - point.x(), via.y() - point.y()) <= via.diameter() / 2) {
                return nets().viaNets().get(index);
            }
        }
        return Optional.empty();
    }

    /**
     * The layers copper of a net reaches at a point: a pad's, wherever on the pad it is — its side's for one on the surface —,
     * a track's or a via's. None of the net there: nothing says, so every layer.
     */
    public List<String> layersReachedAt(Optional<String> net, Point point) {
        List<String> every = pcb.copperLayerIds();
        Set<String> reached = new LinkedHashSet<>();
        List<CopperNets.PlacedPad> pads = nets().pads();
        for (int index = 0; index < pads.size(); index++) {
            CopperNets.PlacedPad pad = pads.get(index);
            if (nets().padNets().get(index).equals(net) && pad.copper().covers(point.x(), point.y())) {
                every.stream().filter(pad::on).forEach(reached::add);
            }
        }
        for (int index = 0; index < pcb.tracks().size(); index++) {
            PcbDrawing.Track track = pcb.tracks().get(index);
            if (nets().trackNets().get(index).equals(net) && track.points().stream()
                    .anyMatch(corner -> Lines.lengthBetween(corner, point) <= track.width() / 2)) {
                reached.add(track.layer());
            }
        }
        for (int index = 0; index < pcb.vias().size(); index++) {
            PcbDrawing.Via via = pcb.vias().get(index);
            if (nets().viaNets().get(index).equals(net) && Math.hypot(via.x() - point.x(), via.y() - point.y()) <= via.diameter() / 2) {
                every.stream().filter(layer -> pcb.reaches(via, layer)).forEach(reached::add);
            }
        }
        return reached.isEmpty() ? every : List.copyOf(reached);
    }

    /** How many connections are still to be laid: a way that truly joins makes it fewer. */
    public int unjoined() {
        return nets().unlaid().size();
    }

    /** The checks' findings that are faults — every one but a notice —: what a laid way may not add to. */
    public List<String> faults() {
        if (faults == null) {
            PcbChecks.Subject subject = new PcbChecks.Subject(pcb, board.footprintsOn(pcb), board.namedOn(pcb), board.partNetsOn(pcb),
                    Optional.of(board.values()));
            faults = PcbChecks.of(subject, nets(), board.checks()).stream().filter(finding -> finding.severity() != FindingSeverity.NOTICE)
                    .map(PcbFinding::message).toList();
        }
        return faults;
    }

    /**
     * The copper a track of {@code net} laid on {@code layer} must keep clear of: every other net's piece of copper the checks
     * measure, and keepouts — the tracks left out where {@code tracks} is false, for what shoves them aside rather than walks
     * round them, and {@code except} left out for being the track that is moving. Without a fixed gap each keeps the gap the
     * rules give it — between two nets the wider of their classes', as the checks measure —, and the pcb's edge is one more,
     * kept at the edge's own gap; with one, everything keeps that and the edge is not asked.
     */
    public List<TrackFence.Obstacle> obstacles(String layer, Optional<String> net, boolean tracks, OptionalInt except, OptionalDouble fixedGap) {
        double own = clearanceOf(net);
        List<TrackFence.Obstacle> obstacles = new ArrayList<>();
        for (CopperPieces.Piece piece : pieces()) {
            boolean ofAnother = net.isEmpty() || !piece.net().equals(net);
            boolean leftOut = (piece.kind() == CopperPieces.Kind.TRACK || piece.kind() == CopperPieces.Kind.DROP)
                              && (!tracks || (except.isPresent() && piece.index() == except.getAsInt()));
            if (piece.onLayer().test(layer) && ofAnother && !leftOut) {
                obstacles.add(new TrackFence.Obstacle(piece.shape(), fixedGap.orElse(Math.max(own, clearanceOf(piece.net())))));
            }
        }
        for (PcbDrawing.Keepout keepout : pcb.keepouts()) {
            if (keepout.layers().contains(layer)) {
                CopperShape.ofPath(keepout.path(), true, 0, curveSteps())
                        .ifPresent(shape -> obstacles.add(new TrackFence.Obstacle(shape, fixedGap.orElse(own))));
            }
        }
        if (fixedGap.isEmpty()) {
            CopperShape.ofPath(pcb.outline(), false, 0, curveSteps())
                    .ifPresent(edge -> obstacles.add(new TrackFence.Obstacle(edge, rules.value(PcbVocabulary.RULE_EDGE_CLEARANCE))));
        }
        return obstacles;
    }

    /** A fence over these obstacles, for a track this wide keeping this clearance, measured as the router measures. */
    public TrackFence fenceOver(double width, double clearance, List<TrackFence.Obstacle> obstacles) {
        RoutingSettings.Routing routing = settings().routing();
        return new TrackFence(width, clearance, obstacles, routing.bucket(), routing.roomLooked(), curveSteps());
    }

    /** What a track of a net, this wide, on a layer must keep clear of, every other track among it. */
    public TrackFence fence(String layer, Optional<String> net, double width) {
        return fence(layer, net, width, OptionalInt.empty(), true);
    }

    /**
     * What a track must keep clear of, on this board.
     *
     * @param except the track of the pcb that is moving, left out of what it must keep clear of
     * @param tracks false for what shoves tracks aside rather than walking round them
     */
    public TrackFence fence(String layer, Optional<String> net, double width, OptionalInt except, boolean tracks) {
        return fences.computeIfAbsent(new FenceKey(layer, net, width, except, tracks),
                key -> fenceOver(width, clearanceOf(net), obstacles(layer, net, tracks, except, OptionalDouble.empty())));
    }

    /** A track as copper, wherever it now runs. */
    public static CopperShape copperOfLine(double width, List<Point> points) {
        return CopperShape.line(points, width / 2);
    }
}
