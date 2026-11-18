package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.Point;
import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Via;
import org.jmouse.cad.pcb.drawing.ViaSize;
import org.jmouse.cad.pcb.geometry.CopperNets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * One connection as one way over every layer, as the search finds it — along a layer where it can, through vias where it is shorter,
 * over a track by a 0 Ω link where the request allows it and nothing else goes —, each stretch pulled tight; its ends fanned out to the
 * lanes they reach, through a neck where the width does not fit at the end.
 */
final class ConnectionWay {

    /** An end as a way starts from it: the lanes it reaches where the width fits there, else a neck out on the first lane one gets out on. */
    private record Fanned(List<Integer> lanes, Point at, Optional<Track> neck) {
    }

    private final AutorouteSession session;

    ConnectionWay(AutorouteSession session) {
        this.session = session;
    }

    /** The layout with the connection's way on it; empty where the search finds none. */
    Optional<PcbDrawing> laid(RoutingWorld world, CopperNets.Unlaid line, List<String> layers, boolean linking, double width) {
        RoutingBoard board = session.board();
        RoutingSettings.Autorouting settings = session.settings();
        PcbDrawing pcb = world.pcb();
        Optional<String> net = Optional.of(line.net());
        ViaSize viaSize = world.rules().viaOf(net);
        List<LayeredWaySearch.Lane> lanes = layers.stream().map(layer -> new LayeredWaySearch.Lane(world.fence(layer, net, width),
                settings.directionOf(layer).map(direction -> new LayeredWaySearch.Preferred(direction, settings.againstDirection())))).toList();
        Optional<Fanned> start = fan(world, layers, net, width, line.from());
        Optional<Fanned> goal = fan(world, layers, net, width, line.to());
        if (start.isEmpty() || goal.isEmpty()) {
            return Optional.empty();
        }
        // A via's copper keeps its gaps on every layer, and its hole a hole's gap from every hole, its own net's among them.
        List<TrackFence> viaFences = Stream.concat(layers.stream().map(layer -> world.fence(layer, net, viaSize.diameter())),
                Stream.of(world.holeFence(viaSize.drill()))).toList();
        Optional<FootprintDrawing> body = linking ? board.linkFootprint() : Optional.empty();
        List<FootprintDrawing.Pad> pads = body.map(FootprintDrawing::pads).orElse(List.of());
        double padSize = pads.size() == 2 ? Math.max(pads.getFirst().width(), pads.getFirst().height()) : 0;
        double reach = pads.size() == 2 ? Math.hypot(pads.get(1).x() - pads.getFirst().x(), pads.get(1).y() - pads.getFirst().y()) : 0;
        Optional<LayeredWaySearch.ViaMove> via = layers.size() > 1
                ? Optional.of(new LayeredWaySearch.ViaMove(viaFences, settings.viaCost(), settings.viaOffEnds())) : Optional.empty();
        Optional<LayeredWaySearch.LinkMove> link = reach > 0 ? Optional.of(new LayeredWaySearch.LinkMove(reach,
                lane -> surface(layers.get(lane)) ? Optional.of(world.fence(layers.get(lane), net, padSize)) : Optional.empty(),
                settings.link().cost())) : Optional.empty();
        Optional<List<LayeredWaySearch.WayPart>> parts = LayeredWaySearch.way(new LayeredWaySearch.Request(lanes, start.get().at(), goal.get().at(),
                start.get().lanes(), goal.get().lanes(), session.angleStep(), via, link), board.settings().routing());
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        List<Track> tracks = new ArrayList<>(pcb.tracks());
        start.get().neck().ifPresent(tracks::add);
        goal.get().neck().ifPresent(tracks::add);
        List<Via> vias = new ArrayList<>(pcb.vias());
        List<Placement> placements = new ArrayList<>(pcb.placements());
        int links = 0;
        for (LayeredWaySearch.WayPart part : parts.get()) {
            switch (part) {
                case LayeredWaySearch.Stretch stretch -> {
                    TrackFence fence = lanes.get(stretch.lane()).fence();
                    List<Point> points = new Tightening(fence, session.angleStep(), List.of(), board.settings().routing()).tightened(stretch.points())
                            .stream().map(session::written).toList();
                    String layer = layers.get(stretch.lane());
                    tracks.add(new Track(layer, width, fence.roundingThatFits(points, session.request().rounding(), layer,
                            board.settings().routing().roundingHalvings()), points));
                }
                case LayeredWaySearch.ViaHop hop -> vias.add(new Via(hop.at().x(), hop.at().y(), viaSize.drill(), viaSize.diameter(),
                        PcbVocabulary.TOP, PcbVocabulary.BOTTOM));
                case LayeredWaySearch.LinkHop hop -> placements.add(linkPlacement(placements, links++, layers.get(hop.lane()), hop.from(), hop.to(),
                        line.net()));
            }
        }
        return Optional.of(pcb.withTracks(tracks).withVias(vias).withPlacements(placements));
    }

    private Optional<Fanned> fan(RoutingWorld world, List<String> layers, Optional<String> net, double width, Point end) {
        List<Integer> reached = new ArrayList<>();
        for (String layer : world.layersReachedAt(net, end)) {
            int lane = layers.indexOf(layer);
            if (lane >= 0) {
                reached.add(lane);
            }
        }
        List<Integer> free = new ArrayList<>();
        Optional<Fanned> necked = Optional.empty();
        for (int lane : reached) {
            Optional<FanOut.FannedEnd> out = FanOut.fannedOut(world, layers.get(lane), net, width, end);
            if (out.isPresent() && out.get().neck().isEmpty()) {
                free.add(lane);
            } else if (necked.isEmpty() && out.isPresent()) {
                FanOut.Neck neck = out.get().neck().get();
                necked = Optional.of(new Fanned(List.of(lane), out.get().at(), Optional.of(new Track(layers.get(lane), neck.width(), 0,
                        List.of(session.written(neck.from()), session.written(neck.to()))))));
            }
        }
        if (!free.isEmpty()) {
            return Optional.of(new Fanned(free, end, Optional.empty()));
        }
        return necked;
    }

    /** A 0 Ω link where a way needs one: its two pads on the two points, on the layer's side, both on the net. */
    private Placement linkPlacement(List<Placement> placements, int among, String layer, Point from, Point to, String net) {
        RoutingSettings.Link link = session.settings().link();
        Set<String> taken = new HashSet<>();
        placements.forEach(placement -> taken.add(placement.designator()));
        int number = among + 1;
        while (taken.contains(link.designator() + number)) {
            number += 1;
        }
        Map<String, String> nets = new LinkedHashMap<>();
        nets.put("1", net);
        nets.put("2", net);
        Point middle = from.halfwayTo(to);
        return new Placement(link.designator() + number, middle.x(), middle.y(), from.y() == to.y() ? 0 : 90,
                PcbVocabulary.BOTTOM.equals(layer) ? PcbVocabulary.BOTTOM : PcbVocabulary.TOP, Optional.empty(), Optional.empty(),
                Optional.of(link.footprint()), nets, Arrangement.FREE, PcbVocabulary.ROLE_LINK);
    }

    private static boolean surface(String layer) {
        return PcbVocabulary.TOP.equals(layer) || PcbVocabulary.BOTTOM.equals(layer);
    }
}
