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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A pcb as the router sees it: one layout of a board, its nets, its copper and its fences worked out once and kept. Everything
 * the router asks of the board it asks here, so laying a track, dragging one, shoving one and routing a whole pcb all measure
 * one way, against the same rules.
 *
 * <p>A world is asked by one router at a time.</p>
 *
 * <p>A world may be of a layout a drag made of another ({@link #draggedFrom}): a drag does not change which net copper is laid
 * for, so its copper keeps the names the board it was dragged from gave it — whatever the layout now joins, as where a part is put
 * down on a track of another net —, while what it joins is still worked out for the layout as it is ({@link #nets}).</p>
 *
 * <p>A world may stand over another ({@link #withTrack}): tracks laid one after another, each on the board as the last left it,
 * share the first world's copper, nets and index — the copper of the tracks laid since left out of it, theirs kept clear of on
 * its own. What the board joins is still worked out afresh for the board as it now is ({@link #nets}).</p>
 */
public final class RoutingWorld {

    private final PcbDrawing                 pcb;
    private final RoutingBoard               board;
    private final NetRules                   rules;
    private final Map<FenceKey, TrackFence>  fences = new HashMap<>();
    /** What a via's hole keeps clear of, by its drill. */
    private final Map<Double, TrackFence>    holeFences = new HashMap<>();
    private final Map<String, LayerCopper>   layers = new HashMap<>();
    private final Map<String, ObstacleCells> layerCells = new HashMap<>();
    /** The world whose copper and index this one shares: itself, for a world of its own. */
    private final RoutingWorld               root;
    /** The tracks laid over the root since, by their places: the root's copper of them left out, theirs kept clear of instead. */
    private final Map<Integer, PcbDrawing.Track> relaid;
    /** The board this layout was dragged from, whose names its copper keeps; none, for a layout named as it joins. */
    private final Optional<RoutingWorld>     namedBy;
    private final Map<Integer, CopperShape>  relaidCopper = new HashMap<>();
    private CopperNets                       nets;
    private List<CopperPieces.Piece>         pieces;
    private List<String>                     faults;
    private CopperNodes                      nodes;

    /** A track asked about: what it is and what is left out of what it meets. */
    private record FenceKey(String layer, Optional<String> net, double width, OptionalInt except) {
    }

    public RoutingWorld(PcbDrawing pcb, RoutingBoard board) {
        this(pcb, board, Optional.empty());
    }

    private RoutingWorld(PcbDrawing pcb, RoutingBoard board, Optional<RoutingWorld> namedBy) {
        this.pcb = pcb;
        this.board = board;
        this.rules = new NetRules(pcb, board.values());
        this.root = this;
        this.relaid = Map.of();
        this.namedBy = namedBy;
    }

    private RoutingWorld(PcbDrawing pcb, RoutingWorld root, Map<Integer, PcbDrawing.Track> relaid) {
        this.pcb = pcb;
        this.board = root.board;
        this.rules = root.rules;
        this.root = root;
        this.relaid = relaid;
        this.namedBy = root.namedBy;
    }

    /** A layout a drag made of the board {@code before}: its copper named as it was there, its pads as the circuit wires them. */
    public static RoutingWorld draggedFrom(RoutingWorld before, PcbDrawing layout) {
        return new RoutingWorld(layout, before.board, Optional.of(before));
    }

    /**
     * This board with one track laid again along these points — its net the same, as a track laid for its net is —: over the
     * same copper, nets and index, so laying the tracks a gesture moved one after another works none of them out again.
     */
    public RoutingWorld withTrack(int index, List<Point> points) {
        PcbDrawing laid = pcb.withTrackPoints(index, points);
        Map<Integer, PcbDrawing.Track> now = new HashMap<>(relaid);
        now.put(index, laid.tracks().get(index));
        return new RoutingWorld(laid, root, Map.copyOf(now));
    }

    /**
     * Another layout of this board worked out from this one, where it differs only in some of its tracks laid again — as a gesture
     * leaves it —, and in no more than the settings let: the copper, nets and index of this board kept, those tracks kept clear of on
     * their own. Empty where it differs in more — a part moved, a track added or taken away —, and has to be worked out whole.
     */
    public Optional<RoutingWorld> relaidAs(PcbDrawing other) {
        if (root != this || other.tracks().size() != pcb.tracks().size() || !other.withTracks(pcb.tracks()).equals(pcb)) {
            return Optional.empty();
        }
        Map<Integer, PcbDrawing.Track> changed = new HashMap<>();
        int most = settings().routing().relaidAtMost();
        for (int index = 0; index < pcb.tracks().size(); index++) {
            PcbDrawing.Track track = other.tracks().get(index);
            if (!track.equals(pcb.tracks().get(index))) {
                changed.put(index, track);
                if (changed.size() > most) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(new RoutingWorld(other, this, Map.copyOf(changed)));
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

    /** Where the copper's lines end and meet: its pads, vias, junctions, arcs' ends, free tips and joints. */
    public CopperNodes nodes() {
        if (nodes == null) {
            nodes = CopperNodes.of(this);
        }
        return nodes;
    }

    /** Which copper is on which net: a board's tracks laid again over it keep their nets, so theirs are the board's. */
    public CopperNets nets() {
        if (root != this) {
            return root.nets();
        }
        if (nets == null) {
            nets = CopperNets.of(pcb, board.footprintsOn(pcb), board.partNetsOn(pcb), board.outlines(), curveSteps());
        }
        return nets;
    }

    /** Every piece of copper the checks measure, a track's drops among its own. */
    public List<CopperPieces.Piece> pieces() {
        if (root != this) {
            return root.pieces();
        }
        if (pieces == null) {
            List<CopperPieces.Piece> joined = CopperPieces.of(pcb, nets(), Teardrops.of(pcb, nets().pads(), board.checks().teardrops()), curveSteps());
            pieces = namedBy.map(was -> joined.stream().map(piece -> piece.withNet(was.netOfPiece(piece))).toList()).orElse(joined);
        }
        return pieces;
    }

    /** The net this board names a piece of another layout's copper by — the piece of its kind at its place —: a pad's, its own. */
    private Optional<String> netOfPiece(CopperPieces.Piece piece) {
        List<Optional<String>> names = switch (piece.kind()) {
            case TRACK, DROP -> nets().trackNets();
            case ARC -> nets().arcNets();
            case DRAWN -> nets().drawnNets();
            case VIA -> nets().viaNets();
            case PAD, HOLE -> List.of();
        };
        return piece.index() < names.size() ? names.get(piece.index()) : piece.net();
    }

    /**
     * Where copper of two nets meets on this board that did not meet on another — the same board before a change —: for each two
     * nets newly joined, the chain of copper joining them, named. Nets any short of the other already joined are not the change's
     * doing. The one rule every way of laying keeps: copper of two nets never meets because of it.
     */
    public List<String> meetingsNotIn(RoutingWorld before) {
        return joinsNotIn(before).stream().map(this::meetingOf).toList();
    }

    /** A meeting of two nets in words, its copper named as on this board: {@code B pad 1 of N would reach C pad 1 of M through Via 1}. */
    public String meetingOf(CopperNets.Joined join) {
        List<CopperNets.Member> chain = join.chain();
        String through = chain.subList(1, chain.size() - 1).stream().map(this::nameOf).collect(Collectors.joining(", "));
        return nameOf(chain.getFirst()) + " of " + join.firstNet() + " would reach " + nameOf(chain.getLast()) + " of " + join.secondNet()
               + (through.isEmpty() ? " directly" : " through " + through);
    }

    /** Where this board's copper joins two nets the board {@code before} did not join: each meeting, and the chain of copper making it. */
    public List<CopperNets.Joined> joinsNotIn(RoutingWorld before) {
        Set<Set<String>> joinedBefore = new HashSet<>();
        for (CopperNets.Short joined : before.nets().shorts()) {
            for (String one : joined.nets()) {
                for (String other : joined.nets()) {
                    if (!one.equals(other)) {
                        joinedBefore.add(Set.of(one, other));
                    }
                }
            }
        }
        List<CopperNets.Joined> made = new ArrayList<>();
        for (CopperNets.Short joined : nets().shorts()) {
            for (CopperNets.Joined join : joined.joins()) {
                if (!joinedBefore.contains(Set.of(join.firstNet(), join.secondNet()))) {
                    made.add(join);
                }
            }
        }
        return made;
    }

    /** A piece of copper as a reader names it: {@code Track 12}, {@code U1 pad 47}, {@code Via 3}, {@code Zone 1}. */
    public String nameOf(CopperNets.Member member) {
        return switch (member.kind()) {
            case PAD -> nets().pads().get(member.index()).placement().printed() + " pad " + nets().pads().get(member.index()).pad().number();
            case TRACK -> "Track " + (member.index() + 1);
            case ARC -> "Arc " + (member.index() + 1);
            case DRAWN -> "Shape " + (member.index() + 1);
            case VIA -> "Via " + (member.index() + 1);
            case ZONE -> "Zone " + (member.index() + 1);
        };
    }

    public double clearanceOf(Optional<String> net) {
        return rules.clearanceOf(net);
    }

    public double trackWidthOf(Optional<String> net) {
        return rules.trackWidthOf(net);
    }

    public Optional<String> netOfTrack(int index) {
        // A track laid again keeps the net it was laid for, and nothing else moved: the root's nets say every track's — the board's
        // it was dragged from, where it was.
        if (namedBy.isPresent() && index < namedBy.get().pcb().tracks().size()) {
            return namedBy.get().netOfTrack(index);
        }
        return root.nets().trackNets().get(index);
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
     * measure, and keepouts — {@code except} left out for being the track that is moving. Without a fixed gap each keeps the gap the
     * rules give it — between two nets the wider of their classes', as the checks measure —, and the pcb's edge is one more,
     * kept at the edge's own gap; with one, everything keeps that and the edge is not asked.
     */
    public List<TrackFence.Obstacle> obstacles(String layer, Optional<String> net, OptionalInt except, OptionalDouble fixedGap) {
        LayerCopper copper = copperOn(layer);
        double[] gaps = gapsOf(copper, net, except, fixedGap);
        List<TrackFence.Obstacle> obstacles = new ArrayList<>();
        for (int number = 0; number < gaps.length; number++) {
            if (!Double.isNaN(gaps[number])) {
                obstacles.add(new TrackFence.Obstacle(copper.shapes().get(number), gaps[number]));
            }
        }
        obstacles.addAll(relaidObstacles(layer, net, except, fixedGap));
        return obstacles;
    }

    /**
     * Something a track keeps clear of, with what it is: a piece of copper — a pad of a part, a track, a via — or, with none, a
     * keepout or the pcb's edge.
     */
    public record NamedObstacle(Optional<CopperPieces.Piece> piece, CopperShape shape, double gap) {
    }

    /**
     * The copper a track of {@code net} laid on {@code layer} keeps clear of, each with what it is: as {@link #obstacles} gives it
     * — but over the root's copper alone, a track laid since as the root had it: what is laid again is the asker's to know.
     */
    public List<NamedObstacle> namedObstacles(String layer, Optional<String> net) {
        LayerCopper copper = copperOn(layer);
        double[] gaps = root.gapsOf(copper, net, OptionalInt.empty(), OptionalDouble.empty());
        List<NamedObstacle> named = new ArrayList<>();
        for (int number = 0; number < gaps.length; number++) {
            if (!Double.isNaN(gaps[number])) {
                Optional<CopperPieces.Piece> piece = number < copper.pieces().size() ? Optional.of(copper.pieces().get(number)) : Optional.empty();
                named.add(new NamedObstacle(piece, copper.shapes().get(number), gaps[number]));
            }
        }
        return named;
    }

    /**
     * The tracks laid over the root since, as obstacles of a track of {@code net} on {@code layer}: those of another net, unless
     * it is {@code except}, each at the gap the rules give it — the same as the root's copper keeps.
     */
    private List<TrackFence.Obstacle> relaidObstacles(String layer, Optional<String> net, OptionalInt except, OptionalDouble fixedGap) {
        if (relaid.isEmpty()) {
            return List.of();
        }
        double own = clearanceOf(net);
        List<TrackFence.Obstacle> obstacles = new ArrayList<>();
        relaid.forEach((index, track) -> {
            Optional<String> trackNet = netOfTrack(index);
            boolean ofAnother = net.isEmpty() || !trackNet.equals(net);
            if (track.layer().equals(layer) && ofAnother && (except.isEmpty() || except.getAsInt() != index)) {
                CopperShape copper = relaidCopper.computeIfAbsent(index, ignored -> CopperShapes.track(track, curveSteps()));
                obstacles.add(new TrackFence.Obstacle(copper, fixedGap.orElse(Math.max(own, clearanceOf(trackNet)))));
            }
        });
        return obstacles;
    }

    /**
     * Everything on a layer a track may have to keep clear of — the pieces of copper on it, its keepouts, the pcb's edge —, in
     * that order: worked out once, and indexed once for every fence over the layer ({@link #cellsOn}).
     *
     * @param pieces the pieces of copper among the shapes, the first of them
     * @param edge   whether the last shape is the pcb's edge
     */
    private record LayerCopper(List<CopperShape> shapes, List<CopperPieces.Piece> pieces, boolean edge) {
    }

    private LayerCopper copperOn(String layer) {
        if (root != this) {
            return root.copperOn(layer);
        }
        return layers.computeIfAbsent(layer, on -> {
            List<CopperPieces.Piece> onLayer = pieces().stream().filter(piece -> piece.onLayer().test(on)).toList();
            List<CopperShape> shapes = new ArrayList<>(onLayer.stream().map(CopperPieces.Piece::shape).toList());
            for (PcbDrawing.Keepout keepout : pcb.keepouts()) {
                if (keepout.layers().contains(on)) {
                    CopperShape.ofPath(keepout.path(), true, 0, curveSteps()).ifPresent(shapes::add);
                }
            }
            Optional<CopperShape> edge = CopperShape.ofPath(pcb.outline(), false, 0, curveSteps());
            edge.ifPresent(shapes::add);
            return new LayerCopper(List.copyOf(shapes), onLayer, edge.isPresent());
        });
    }

    /** A layer's shapes by the cells of the board, shared by every fence over the layer. */
    private ObstacleCells cellsOn(String layer) {
        if (root != this) {
            return root.cellsOn(layer);
        }
        return layerCells.computeIfAbsent(layer, on -> new ObstacleCells(copperOn(on).shapes(), settings().routing().bucket()));
    }

    /**
     * The gap a track of {@code net} keeps from each of a layer's shapes, by its number; {@code NaN} where the shape is not its
     * obstacle — its own net's copper, the track left out, the edge where the gap is fixed (see {@link #obstacles}).
     */
    private double[] gapsOf(LayerCopper copper, Optional<String> net, OptionalInt except, OptionalDouble fixedGap) {
        double own = clearanceOf(net);
        double[] gaps = new double[copper.shapes().size()];
        int number = 0;
        for (CopperPieces.Piece piece : copper.pieces()) {
            boolean ofAnother = net.isEmpty() || !piece.net().equals(net);
            // A track laid over the root since is kept clear of as it now runs (relaidObstacles), not as the root had it.
            boolean leftOut = (piece.kind() == CopperPieces.Kind.TRACK || piece.kind() == CopperPieces.Kind.DROP)
                              && (except.isPresent() && piece.index() == except.getAsInt() || relaid.containsKey(piece.index()));
            gaps[number++] = ofAnother && !leftOut ? fixedGap.orElse(Math.max(own, clearanceOf(piece.net()))) : Double.NaN;
        }
        int keepouts = copper.shapes().size() - copper.pieces().size() - (copper.edge() ? 1 : 0);
        for (int keepout = 0; keepout < keepouts; keepout++) {
            gaps[number++] = fixedGap.orElse(own);
        }
        if (copper.edge()) {
            gaps[number] = fixedGap.isEmpty() ? rules.value(PcbVocabulary.RULE_EDGE_CLEARANCE) : Double.NaN;
        }
        return gaps;
    }

    /** A fence over these obstacles, for a track this wide keeping this clearance, measured as the router measures. */
    public TrackFence fenceOver(double width, double clearance, List<TrackFence.Obstacle> obstacles) {
        RoutingSettings.Routing routing = settings().routing();
        return new TrackFence(width, clearance, obstacles, routing.bucket(), curveSteps());
    }

    /**
     * What the hole of a via drilled this wide keeps clear of: every hole on the board — its plated pads', its vias', its bare
     * holes —, whatever its net, by the rules' hole clearance. A via of its own net is no copper in its way, but its hole is.
     */
    public TrackFence holeFence(double drill) {
        return holeFences.computeIfAbsent(drill, ignored -> {
            double keep = rules().value(PcbVocabulary.RULE_HOLE_CLEARANCE);
            List<TrackFence.Obstacle> holes = new ArrayList<>();
            for (CopperNets.PlacedPad pad : nets().pads()) {
                if (pad.pad().drillWidth() > 0) {
                    holes.add(new TrackFence.Obstacle(CopperShapes.hole(pad.placement(), pad.pad()), keep));
                }
            }
            for (PcbDrawing.Via via : pcb.vias()) {
                holes.add(new TrackFence.Obstacle(CopperShape.circle(via.x(), via.y(), via.drill() / 2), keep));
            }
            for (PcbDrawing.Hole hole : pcb.holes()) {
                holes.add(new TrackFence.Obstacle(CopperShape.circle(hole.x(), hole.y(), hole.drill() / 2), keep));
            }
            return fenceOver(drill, keep, holes);
        });
    }

    /** What a track of a net, this wide, on a layer must keep clear of, every other track among it. */
    public TrackFence fence(String layer, Optional<String> net, double width) {
        return fence(layer, net, width, OptionalInt.empty());
    }

    /**
     * What a track must keep clear of, on this board.
     *
     * @param except the track of the pcb that is moving, left out of what it must keep clear of
     */
    public TrackFence fence(String layer, Optional<String> net, double width, OptionalInt except) {
        // Every fence over a layer shares the layer's shapes and cells: it is the gaps it keeps, no index of its own.
        return fences.computeIfAbsent(new FenceKey(layer, net, width, except), key -> {
            LayerCopper copper = copperOn(layer);
            return new TrackFence(width, clearanceOf(net), copper.shapes(), gapsOf(copper, net, except, OptionalDouble.empty()),
                                  cellsOn(layer), curveSteps())
                    .alsoKeepingClearOf(relaidObstacles(layer, net, except, OptionalDouble.empty()), settings().routing().bucket());
        });
    }

    /**
     * What a track of the board keeps clear of where it would run as this one does, every other track among it and its own ends let
     * leave where they stand — copper they already stand too near need not be left at once.
     */
    public TrackFence fenceOfTrack(PcbDrawing.Track track, int index, Optional<String> net) {
        List<Point> line = track.points();
        return fence(track.layer(), net, track.width(), OptionalInt.of(index)).leaving(List.of(line.getFirst(), line.getLast()));
    }

    /** A track as copper, wherever it now runs. */
    public static CopperShape copperOfLine(double width, List<Point> points) {
        return CopperShape.line(points, width / 2);
    }
}
