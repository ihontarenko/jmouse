package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.Hole;
import org.jmouse.cad.pcb.drawing.Keepout;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;
import org.jmouse.geometry.index.BoxGrid;
import org.jmouse.geometry.index.BoxIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * What a track keeps clear of in one world, and the fences that keep it so: a layer's copper, keepouts and edge worked out once and
 * indexed once — shared by every world laid over the same root —, the gap a track of a net keeps from each, and the fences over them,
 * kept by what they are asked for.
 */
final class WorldFences {

    /** A track asked about: what it is and what is left out of what it meets. */
    private record FenceKey(String layer, Optional<String> net, double width, OptionalInt except) {
    }

    private final RoutingWorld               world;
    private final Map<FenceKey, TrackFence>  fences = new HashMap<>();
    /** What a via's hole keeps clear of, by its drill. */
    private final Map<Double, TrackFence>    holeFences = new HashMap<>();
    private final Map<String, LayerCopper>   layers = new HashMap<>();
    private final Map<String, BoxIndex>      layerCells = new HashMap<>();
    private final Map<Integer, CopperShape>  relaidCopper = new HashMap<>();

    WorldFences(RoutingWorld world) {
        this.world = world;
    }

    /**
     * The copper a track of {@code net} laid on {@code layer} must keep clear of: every other net's piece of copper the checks
     * measure, and keepouts — {@code except} left out for being the track that is moving. Without a fixed gap each keeps the gap the
     * rules give it — between two nets the wider of their classes', as the checks measure —, and the pcb's edge is one more,
     * kept at the edge's own gap; with one, everything keeps that and the edge is not asked.
     */
    List<TrackFence.Obstacle> obstacles(String layer, Optional<String> net, OptionalInt except, OptionalDouble fixedGap) {
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
     * The copper a track of {@code net} laid on {@code layer} keeps clear of, each with what it is: as {@link #obstacles} gives it
     * — but over the root's copper alone, a track laid since as the root had it: what is laid again is the asker's to know.
     */
    List<RoutingWorld.NamedObstacle> namedObstacles(String layer, Optional<String> net) {
        LayerCopper copper = copperOn(layer);
        double[] gaps = world.root().fences().gapsOf(copper, net, OptionalInt.empty(), OptionalDouble.empty());
        List<RoutingWorld.NamedObstacle> named = new ArrayList<>();
        for (int number = 0; number < gaps.length; number++) {
            if (!Double.isNaN(gaps[number])) {
                Optional<CopperPieces.Piece> piece = number < copper.pieces().size() ? Optional.of(copper.pieces().get(number)) : Optional.empty();
                named.add(new RoutingWorld.NamedObstacle(piece, copper.shapes().get(number), gaps[number]));
            }
        }
        return named;
    }

    /**
     * The tracks laid over the root since, as obstacles of a track of {@code net} on {@code layer}: those of another net, unless
     * it is {@code except}, each at the gap the rules give it — the same as the root's copper keeps.
     */
    private List<TrackFence.Obstacle> relaidObstacles(String layer, Optional<String> net, OptionalInt except, OptionalDouble fixedGap) {
        if (world.relaid().isEmpty()) {
            return List.of();
        }
        double own = world.clearanceOf(net);
        List<TrackFence.Obstacle> obstacles = new ArrayList<>();
        world.relaid().forEach((index, track) -> {
            Optional<String> trackNet = world.netOfTrack(index);
            boolean ofAnother = net.isEmpty() || !trackNet.equals(net);
            if (track.layer().equals(layer) && ofAnother && (except.isEmpty() || except.getAsInt() != index)) {
                CopperShape copper = relaidCopper.computeIfAbsent(index, ignored -> CopperShapes.track(track, world.curveSteps()));
                obstacles.add(new TrackFence.Obstacle(copper, fixedGap.orElse(Math.max(own, world.clearanceOf(trackNet)))));
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
        if (world.root() != world) {
            return world.root().fences().copperOn(layer);
        }
        return layers.computeIfAbsent(layer, on -> {
            List<CopperPieces.Piece> onLayer = world.pieces().stream().filter(piece -> piece.onLayer().test(on)).toList();
            List<CopperShape> shapes = new ArrayList<>(onLayer.stream().map(CopperPieces.Piece::shape).toList());
            for (Keepout keepout : world.pcb().keepouts()) {
                if (keepout.layers().contains(on)) {
                    CopperShape.ofPath(keepout.path(), true, 0, world.curveSteps()).ifPresent(shapes::add);
                }
            }
            Optional<CopperShape> edge = CopperShape.ofPath(world.pcb().outline(), false, 0, world.curveSteps());
            edge.ifPresent(shapes::add);
            return new LayerCopper(List.copyOf(shapes), onLayer, edge.isPresent());
        });
    }

    /** A layer's shapes by the cells of the board, shared by every fence over the layer. */
    private BoxIndex cellsOn(String layer) {
        if (world.root() != world) {
            return world.root().fences().cellsOn(layer);
        }
        return layerCells.computeIfAbsent(layer, on -> BoxGrid.of(copperOn(on).shapes().stream().map(CopperShape::box).toList(),
                world.settings().routing().fenceCells()));
    }

    /**
     * The gap a track of {@code net} keeps from each of a layer's shapes, by its number; {@code NaN} where the shape is not its
     * obstacle — its own net's copper, the track left out, the edge where the gap is fixed (see {@link #obstacles}).
     */
    private double[] gapsOf(LayerCopper copper, Optional<String> net, OptionalInt except, OptionalDouble fixedGap) {
        double own = world.clearanceOf(net);
        double[] gaps = new double[copper.shapes().size()];
        int number = 0;
        for (CopperPieces.Piece piece : copper.pieces()) {
            boolean ofAnother = net.isEmpty() || !piece.net().equals(net);
            // A track laid over the root since is kept clear of as it now runs (relaidObstacles), not as the root had it.
            boolean leftOut = (piece.kind() == CopperPieces.Kind.TRACK || piece.kind() == CopperPieces.Kind.DROP)
                              && (except.isPresent() && piece.index() == except.getAsInt() || world.relaid().containsKey(piece.index()));
            gaps[number++] = ofAnother && !leftOut ? fixedGap.orElse(Math.max(own, world.clearanceOf(piece.net()))) : Double.NaN;
        }
        int keepouts = copper.shapes().size() - copper.pieces().size() - (copper.edge() ? 1 : 0);
        for (int keepout = 0; keepout < keepouts; keepout++) {
            gaps[number++] = fixedGap.orElse(own);
        }
        if (copper.edge()) {
            gaps[number] = fixedGap.isEmpty() ? world.rules().value(PcbVocabulary.RULE_EDGE_CLEARANCE) : Double.NaN;
        }
        return gaps;
    }

    /** A fence over these obstacles, for a track this wide keeping this clearance, measured as the router measures. */
    TrackFence fenceOver(double width, double clearance, List<TrackFence.Obstacle> obstacles) {
        RoutingSettings.Routing routing = world.settings().routing();
        return new TrackFence(width, clearance, obstacles, routing.fenceCells(), world.curveSteps());
    }

    /**
     * What the hole of a via drilled this wide keeps clear of: every hole on the board — its plated pads', its vias', its bare
     * holes —, whatever its net, by the rules' hole clearance. A via of its own net is no copper in its way, but its hole is.
     */
    TrackFence holeFence(double drill) {
        return holeFences.computeIfAbsent(drill, ignored -> {
            double keep = world.rules().value(PcbVocabulary.RULE_HOLE_CLEARANCE);
            List<TrackFence.Obstacle> holes = new ArrayList<>();
            for (CopperNets.PlacedPad pad : world.nets().pads()) {
                if (pad.pad().drillWidth() > 0) {
                    holes.add(new TrackFence.Obstacle(CopperShapes.hole(pad.placement(), pad.pad()), keep));
                }
            }
            for (Via via : world.pcb().vias()) {
                holes.add(new TrackFence.Obstacle(CopperShape.circle(via.x(), via.y(), via.drill() / 2), keep));
            }
            for (Hole hole : world.pcb().holes()) {
                holes.add(new TrackFence.Obstacle(CopperShape.circle(hole.x(), hole.y(), hole.drill() / 2), keep));
            }
            return fenceOver(drill, keep, holes);
        });
    }

    /** What a track of a net, this wide, on a layer must keep clear of, every other track among it. */
    TrackFence fence(String layer, Optional<String> net, double width) {
        return fence(layer, net, width, OptionalInt.empty());
    }

    /**
     * What a track must keep clear of, on this board.
     *
     * @param except the track of the pcb that is moving, left out of what it must keep clear of
     */
    TrackFence fence(String layer, Optional<String> net, double width, OptionalInt except) {
        // Every fence over a layer shares the layer's shapes and cells: it is the gaps it keeps, no index of its own.
        return fences.computeIfAbsent(new FenceKey(layer, net, width, except), key -> {
            LayerCopper copper = copperOn(layer);
            return new TrackFence(width, world.clearanceOf(net), copper.shapes(), gapsOf(copper, net, except, OptionalDouble.empty()),
                                  cellsOn(layer), world.curveSteps())
                    .alsoKeepingClearOf(relaidObstacles(layer, net, except, OptionalDouble.empty()), world.settings().routing().fenceCells());
        });
    }

    /**
     * What a track of the board keeps clear of where it would run as this one does, every other track among it and its own ends let
     * leave where they stand — copper they already stand too near need not be left at once.
     */
    TrackFence fenceOfTrack(Track track, int index, Optional<String> net) {
        List<Point> line = track.points();
        return fence(track.layer(), net, track.width(), OptionalInt.of(index)).leaving(List.of(line.getFirst(), line.getLast()));
    }

}
