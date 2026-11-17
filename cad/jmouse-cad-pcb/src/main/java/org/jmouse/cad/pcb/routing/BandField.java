package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperPieces;
import org.jmouse.geometry.taut.Obstacle;
import org.jmouse.geometry.taut.ObstacleField;
import org.jmouse.geometry.taut.UnitedObstacles;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.jmouse.geometry.Areas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * What one band keeps clear of a share of the way along a drag, as obstacles of the plane: every other net's copper on its layer
 * — the moved parts' pads where they then stand, the other bands as they then run, but for the still ones a moving band pushes
 * aside —, each grown by its gap and half the band's width ({@link KeptOut}); and the pads the band ends on, its own ground, cut out
 * of them.
 *
 * <p>The copper that stands still through the drag is united once for the bands of a net and a width on a layer, over the region
 * they come past — again only where a band comes past it further —; what moves is added to it each step.</p>
 */
final class BandField {

    /** What a band does to the other bands: keeps clear of them, or lays itself among the solid copper alone and pushes them. */
    enum OtherBands {
        KEPT_CLEAR, PUSHED
    }

    /** A layer's copper a net keeps clear of, asked once. */
    private record Asked(String layer, Optional<String> net) {
    }

    /** The bands that keep clear of the still copper alike: of one net and one width, on one layer. */
    private record Alike(String layer, Optional<String> net, double width) {
    }

    /** The still copper bands alike keep clear of, united over a region. */
    private record Still(Envelope covered, UnitedObstacles united) {
    }

    private final RoutingWorld                                    world;
    private final DragMotion                                      motion;
    private final RoutingSettings.Banding                         settings;
    private final Set<Integer>                                    banded = new HashSet<>();
    /** The tracks that go whole with a moved part, each with its part: they move with it step by step, as its pads do. */
    private final Map<Integer, String>                            whole;
    private final Map<Asked, List<RoutingWorld.NamedObstacle>>    named  = new HashMap<>();
    private final Map<Alike, Still>                               still  = new HashMap<>();

    /**
     * @param world  the board as the drag leaves it, whatever the bands do
     * @param bands  the drag's bands: their tracks are kept clear of as the bands run, never as they stood
     * @param whole  the tracks that go whole with a moved part, each with its part
     */
    BandField(RoutingWorld world, DragMotion motion, List<Band> bands, Map<Integer, String> whole) {
        this.world = world;
        this.motion = motion;
        this.whole = Map.copyOf(whole);
        this.settings = world.settings().banding();
        bands.forEach(band -> banded.addAll(band.tracks()));
    }

    /** What a band running a path keeps clear of, a share of the way along, among the bands as they now run. */
    ObstacleField of(Band band, List<Point> path, List<Band> bands, double share, OtherBands others) {
        Envelope window = Plane.FACTORY.createLineString(Plane.coordinatesOf(path).toArray(Coordinate[]::new)).getEnvelopeInternal();
        window.expandBy(settings.margin() + band.width() + world.clearanceOf(band.net()) * 2);
        List<RoutingWorld.NamedObstacle> layerCopper = named.computeIfAbsent(new Asked(band.layer(), band.net()),
                asked -> world.namedObstacles(asked.layer(), asked.net()));
        UnitedObstacles standing = stillFor(band, layerCopper, window);
        List<Obstacle> obstacles = new ArrayList<>();
        for (RoutingWorld.NamedObstacle obstacle : layerCopper) {
            if (isBanded(obstacle.piece()) || carriedBy(obstacle).isEmpty()) {
                continue;
            }
            obstacleOf(placedPieces(obstacle, share), grownBy(obstacle, band), window).ifPresent(obstacles::add);
        }
        for (Band other : others == OtherBands.PUSHED ? List.<Band>of() : bands) {
            // A moving band pushes a still one ahead of it: the still one keeps clear of it, never it of the still one.
            boolean pushedAside = band.moving() && !other.moving();
            if (other != band && !pushedAside && other.layer().equals(band.layer()) && (band.net().isEmpty() || !other.net().equals(band.net()))) {
                double gap = Math.max(world.clearanceOf(band.net()), world.clearanceOf(other.net()));
                List<Point> line = other.fullPath();
                List<Polygon> parts = KeptOut.grown(KeptOut.stretchesOf(line), other.width() / 2 + gap + band.width() / 2 - KeptOut.EASED, window);
                if (!parts.isEmpty()) {
                    // Told apart by its first end, which goes with its pad or stands: never by a corner, which may jump as it is pulled again.
                    obstacles.add(new Obstacle(parts, Plane.coordinateOf(line.getFirst())));
                }
            }
        }
        return ObstacleField.of(standing, obstacles, groundOf(band, path, share), KeptOut.SLACK, Plane.FACTORY);
    }

    /**
     * The still copper a band keeps clear of, united over a region holding the window: the one united before for bands alike where
     * it holds the window, else united again over a region past the window by the margin — room to move in before the next time.
     */
    private UnitedObstacles stillFor(Band band, List<RoutingWorld.NamedObstacle> layerCopper, Envelope window) {
        Alike alike = new Alike(band.layer(), band.net(), band.width());
        Still kept = still.get(alike);
        if (kept != null && kept.covered().contains(window)) {
            return kept.united();
        }
        Envelope covered = new Envelope(window);
        covered.expandBy(settings.margin());
        if (kept != null) {
            covered.expandToInclude(kept.covered());
        }
        List<Obstacle> obstacles = new ArrayList<>();
        for (RoutingWorld.NamedObstacle obstacle : layerCopper) {
            if (!isBanded(obstacle.piece()) && carriedBy(obstacle).isEmpty()) {
                obstacleOf(obstacle.shape().pieces(), grownBy(obstacle, band), covered).ifPresent(obstacles::add);
            }
        }
        UnitedObstacles united = UnitedObstacles.of(obstacles, KeptOut.SLACK, Plane.FACTORY);
        still.put(alike, new Still(covered, united));
        return united;
    }

    /** How far a band keeps a piece of copper off: the copper's own half width, the gap between them and half the band's width, eased. */
    private static double grownBy(RoutingWorld.NamedObstacle obstacle, Band band) {
        return obstacle.shape().radius() + obstacle.gap() + band.width() / 2 - KeptOut.EASED;
    }

    /** A piece of copper as an obstacle where it comes within a region: its pieces grown round, told apart by its first point. */
    private static Optional<Obstacle> obstacleOf(List<double[]> pieces, double grow, Envelope region) {
        List<Polygon> parts = KeptOut.grown(pieces, grow, region);
        return parts.isEmpty() ? Optional.empty() : Optional.of(new Obstacle(parts, new Coordinate(pieces.getFirst()[0], pieces.getFirst()[1])));
    }

    /** The moved part an obstacle goes with, if it moves at all. */
    private Optional<String> carriedBy(RoutingWorld.NamedObstacle obstacle) {
        return obstacle.piece().flatMap(this::partCarrying).filter(motion::moves);
    }

    /** The moved part a piece of copper goes with: a pad's own part, or the part a whole track stands on; none for other copper. */
    private Optional<String> partCarrying(CopperPieces.Piece piece) {
        return switch (piece.kind()) {
            case PAD -> piece.part();
            case TRACK, DROP -> Optional.ofNullable(whole.get(piece.index()));
            default -> Optional.empty();
        };
    }

    private boolean isBanded(Optional<CopperPieces.Piece> piece) {
        return piece.filter(found -> (found.kind() == CopperPieces.Kind.TRACK || found.kind() == CopperPieces.Kind.DROP)
                                     && banded.contains(found.index())).isPresent();
    }

    /**
     * An obstacle's pieces where they stand a share of the way along: a moved part's pad, or a track that goes whole with it, carried
     * there from where the part is let go — never standing where the part is not yet.
     */
    private List<double[]> placedPieces(RoutingWorld.NamedObstacle obstacle, double share) {
        Optional<String> part = carriedBy(obstacle);
        if (part.isEmpty() || share >= 1) {
            return obstacle.shape().pieces();
        }
        List<double[]> placed = new ArrayList<>();
        for (double[] piece : obstacle.shape().pieces()) {
            Point from = motion.brought(part.get(), new Point(piece[0], piece[1]), share);
            Point to = motion.brought(part.get(), new Point(piece[2], piece[3]), share);
            placed.add(new double[] {from.x(), from.y(), to.x(), to.y()});
        }
        return placed;
    }

    /** The pads the band's ends stand on — of its net, or any where it has none —, where they stand: its own ground. */
    private Geometry groundOf(Band band, List<Point> path, double share) {
        List<Geometry> ground = new ArrayList<>();
        List<CopperNets.PlacedPad> pads = world.nets().pads();
        List<Optional<String>> nets = world.nets().padNets();
        for (Point end : List.of(path.getFirst(), path.getLast())) {
            for (int index = 0; index < pads.size(); index++) {
                CopperNets.PlacedPad pad = pads.get(index);
                boolean ofIt = band.net().isEmpty() || nets.get(index).isEmpty() || nets.get(index).equals(band.net());
                if (!ofIt || !pad.on(band.layer())) {
                    continue;
                }
                Geometry shape = motion.moves(pad.designator()) && share < 1
                        ? Plane.moved(pad.shape(), point -> motion.brought(pad.designator(), point, share)) : pad.shape();
                if (shape.covers(Plane.FACTORY.createPoint(Plane.coordinateOf(end)))) {
                    ground.add(shape);
                }
            }
        }
        return ground.isEmpty() ? Plane.FACTORY.createPolygon() : Areas.unionOf(ground);
    }

    /** How far apart two bands' centre lines keep: their gap and both their half widths. */
    double keptApart(Band band, Band other) {
        return Math.max(world.clearanceOf(band.net()), world.clearanceOf(other.net())) + band.width() / 2 + other.width() / 2;
    }

    /** The least a band keeps clear by — its gap and half its width —: what a step of the drag stays under. */
    double leastReach(List<Band> bands) {
        return bands.stream().mapToDouble(band -> world.clearanceOf(band.net()) + band.width() / 2).min().orElse(Double.POSITIVE_INFINITY);
    }
}

