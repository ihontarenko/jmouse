package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.drawing.Arrangement;
import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;
import org.jmouse.cad.pcb.geometry.CopperShapes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The pcb's unrouted connections laid by the router every track is laid by, one by one, shortest first: each along a layer both
 * its ends reach — the layer whose direction suits it first, so the layers cross rather than fight for the same channels —,
 * walked round what is already there; one no layer carries taken under through a via. A track is kept only where the pcb's
 * checks find no more than they did without it, so what is laid never breaks a rule. A connection that cannot be laid so is
 * retried by finding its way as if this routing had laid nothing, lifting every route that way runs into, laying it and laying
 * the lifted ones again; failing that, by lifting one route near it at a time. Kept only when all of them lie; a track the
 * reader drew is never lifted. What still cannot be laid is left in the ratsnest and said.
 */
public final class Autorouter {

    /** How a connection may cross what is in its way on its layer. */
    public enum Crossing {
        VIAS, LINKS
    }

    /** What the router has just done. */
    public enum Step {
        TRYING, ATTEMPT_REFUSED, LAID, REFUSED, LIFTED, RESTORED, ROUND
    }

    /**
     * A routing asked for.
     *
     * @param angleStep the directions its tracks may take, as the reader lays tracks: 90, or the settings' own for any other
     * @param rounding  the radius its tracks' corners are rounded with
     * @param onlyNets  only the connections of these nets are laid; empty, every one
     * @param stopped   asked between connections: once it says so, what was laid so far is answered
     * @param moments   told of every moment as it happens
     */
    public record Request(PcbDrawing pcb, double angleStep, double rounding, Crossing crossing, Set<String> onlyNets, BooleanSupplier stopped,
                          Consumer<Moment> moments) {

        public Request {
            onlyNets = Set.copyOf(onlyNets);
        }
    }

    /**
     * What the router has just done, and how far it has come.
     *
     * @param pcb     the pcb as it stands at this moment, the copper of this routing on it
     * @param line    the connection it is about; empty for a new round
     * @param attempt the copper given up, for the moment it is shown
     * @param reason  why it was given up, or the connection refused, in the checks' words
     * @param refused the connections refused so far in this round
     */
    public record Moment(Step step, PcbDrawing pcb, Optional<CopperNets.Unlaid> line, List<PcbDrawing.Track> attempt, Optional<String> reason,
                         List<CopperNets.Unlaid> refused, int laid, int tried, int total, int round) {
    }

    /**
     * @param laid     how many connections were laid
     * @param unrouted the connections still unrouted: those no layer and no via could carry within the rules
     * @param stopped  whether it stopped because it was asked to, before every connection was tried
     */
    public record Routed(PcbDrawing pcb, int laid, List<CopperNets.Unlaid> unrouted, boolean stopped) {
    }

    /** How many layouts are kept worked out before they are let go: a routing comes to a new one with each step. */
    private static final int KEPT_WORLDS = 64;

    private final RoutingBoard                      board;
    private final Request                           request;
    private final RoutingSettings.Autorouting       settings;
    private final Map<PcbDrawing, RoutingWorld>     worlds = new IdentityHashMap<>();
    private final long                              until;
    private int                                     tried;
    private int                                     total;
    private int                                     laidBefore;
    private int                                     round;
    private int                                     laidNow;
    private List<CopperNets.Unlaid>                 refusedNow = List.of();

    private Autorouter(RoutingBoard board, Request request) {
        this.board = board;
        this.request = request;
        this.settings = board.settings().autorouting();
        this.until = System.nanoTime() + (long) (settings.budgetSeconds() * 1e9);
    }

    /**
     * The pcb with its unrouted connections laid, on every layer it has, each within the rules: shortest first, the refused
     * retried by ripping up what this routing laid near them. What is left is tried again from the start, it going first, for as
     * long as that lays more. Tracks take their net's width, vias their net's size, as a track drawn by hand.
     */
    public static Routed route(RoutingBoard board, Request request) {
        return new Autorouter(board, request).routed();
    }

    private RoutingWorld worldOf(PcbDrawing pcb) {
        if (worlds.size() > KEPT_WORLDS) {
            worlds.clear();
        }
        return worlds.computeIfAbsent(pcb, layout -> new RoutingWorld(layout, board));
    }

    private boolean wanted(CopperNets.Unlaid line) {
        return request.onlyNets().isEmpty() || request.onlyNets().contains(line.net());
    }

    private List<CopperNets.Unlaid> left(PcbDrawing pcb) {
        return worldOf(pcb).nets().unlaid().stream().filter(this::wanted).toList();
    }

    private boolean outOfTime() {
        return request.stopped().getAsBoolean() || System.nanoTime() > until;
    }

    private void tell(Step step, PcbDrawing pcb, Optional<CopperNets.Unlaid> line, List<PcbDrawing.Track> attempt, Optional<String> reason) {
        request.moments().accept(new Moment(step, pcb, line, attempt, reason, refusedNow, Math.max(laidNow, laidBefore), tried, total, round));
    }

    private void tell(Step step, PcbDrawing pcb, Optional<CopperNets.Unlaid> line) {
        tell(step, pcb, line, List.of(), Optional.empty());
    }

    private double angleStep() {
        return request.angleStep() == 90 ? 90 : settings.angleStep();
    }

    private static double lengthOf(CopperNets.Unlaid line) {
        return Lines.lengthBetween(line.from(), line.to());
    }

    /** A point written to the settings' places: a step's arithmetic leaves no more than that worth saying. */
    private Point written(Point point) {
        return new Point(Lines.writtenTo(point.x(), settings.decimals()), Lines.writtenTo(point.y(), settings.decimals()));
    }

    private static boolean surface(String layer) {
        return PcbDrawing.TOP.equals(layer) || PcbDrawing.BOTTOM.equals(layer);
    }

    /** An end as a way starts from it: the lanes it reaches where the width fits there, else a neck out on the first lane one gets out on. */
    private record Fanned(List<Integer> lanes, Point at, Optional<PcbDrawing.Track> neck) {
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
                necked = Optional.of(new Fanned(List.of(lane), out.get().at(), Optional.of(new PcbDrawing.Track(layers.get(lane), neck.width(), 0,
                        List.of(written(neck.from()), written(neck.to()))))));
            }
        }
        if (!free.isEmpty()) {
            return Optional.of(new Fanned(free, end, Optional.empty()));
        }
        return necked;
    }

    /**
     * One connection as one way over every layer, as the search finds it — along a layer where it can, through vias where it is
     * shorter, over a track by a 0 Ω link where the request allows it and nothing else goes —, each stretch pulled tight.
     */
    private Optional<PcbDrawing> wayOf(RoutingWorld world, CopperNets.Unlaid line, List<String> layers, boolean linking, double width) {
        PcbDrawing pcb = world.pcb();
        Optional<String> net = Optional.of(line.net());
        PcbDrawing.ViaSize viaSize = world.rules().viaOf(net);
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
                start.get().lanes(), goal.get().lanes(), angleStep(), via, link), board.settings().routing());
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        List<PcbDrawing.Track> tracks = new ArrayList<>(pcb.tracks());
        start.get().neck().ifPresent(tracks::add);
        goal.get().neck().ifPresent(tracks::add);
        List<PcbDrawing.Via> vias = new ArrayList<>(pcb.vias());
        List<PcbDrawing.Placement> placements = new ArrayList<>(pcb.placements());
        int links = 0;
        for (LayeredWaySearch.WayPart part : parts.get()) {
            switch (part) {
                case LayeredWaySearch.Stretch stretch -> {
                    TrackFence fence = lanes.get(stretch.lane()).fence();
                    List<Point> points = new Tightening(fence, angleStep(), List.of(), board.settings().routing()).tightened(stretch.points())
                            .stream().map(this::written).toList();
                    String layer = layers.get(stretch.lane());
                    tracks.add(new PcbDrawing.Track(layer, width, fence.roundingThatFits(points, request.rounding(), layer), points));
                }
                case LayeredWaySearch.ViaHop hop -> vias.add(new PcbDrawing.Via(hop.at().x(), hop.at().y(), viaSize.drill(), viaSize.diameter(),
                        PcbDrawing.TOP, PcbDrawing.BOTTOM));
                case LayeredWaySearch.LinkHop hop -> placements.add(linkPlacement(placements, links++, layers.get(hop.lane()), hop.from(), hop.to(), line.net()));
            }
        }
        return Optional.of(pcb.withTracks(tracks).withVias(vias).withPlacements(placements));
    }

    /** A 0 Ω link where a way needs one: its two pads on the two points, on the layer's side, both on the net. */
    private PcbDrawing.Placement linkPlacement(List<PcbDrawing.Placement> placements, int among, String layer, Point from, Point to, String net) {
        RoutingSettings.Link link = settings.link();
        Set<String> taken = new HashSet<>();
        placements.forEach(placement -> taken.add(placement.designator()));
        int number = among + 1;
        while (taken.contains(link.designator() + number)) {
            number += 1;
        }
        Map<String, String> nets = new LinkedHashMap<>();
        nets.put("1", net);
        nets.put("2", net);
        return new PcbDrawing.Placement(link.designator() + number, (from.x() + to.x()) / 2, (from.y() + to.y()) / 2, from.y() == to.y() ? 0 : 90,
                PcbDrawing.BOTTOM.equals(layer) ? PcbDrawing.BOTTOM : PcbDrawing.TOP, Optional.empty(), Optional.empty(), Optional.of(link.footprint()),
                nets, Arrangement.FREE, PcbVocabulary.ROLE_LINK);
    }

    /** One connection laid: the way kept only where the checks find nothing new in it and it truly joins what it was to join. */
    private Optional<PcbDrawing> laidConnection(PcbDrawing pcb, CopperNets.Unlaid line, List<String> layers) {
        RoutingWorld world = worldOf(pcb);
        List<String> before = world.faults();
        int unjoined = world.unjoined();
        // Its net's width first, then thin all the way, as a person gives up a width before a link: what fits with least given up.
        double width = world.trackWidthOf(Optional.of(line.net()));
        double least = world.rules().value(PcbVocabulary.RULE_LEAST_TRACK_WIDTH);
        List<Double> widths = least > 0 && least < width ? List.of(width, least) : List.of(width);
        List<Boolean> linkings = request.crossing() == Crossing.LINKS ? List.of(false, true) : List.of(false);
        for (boolean linking : linkings) {
            for (double tried : widths) {
                Optional<PcbDrawing> laid = wayOf(world, line, layers, linking, tried);
                if (laid.isEmpty()) {
                    tell(Step.ATTEMPT_REFUSED, pcb, Optional.of(line), List.of(), Optional.of("No way found"));
                    continue;
                }
                RoutingWorld laidOn = worldOf(laid.get());
                List<String> faults = laidOn.faults();
                boolean joins = laidOn.unjoined() < unjoined;
                // Copper of two nets never meets because of a way laid — however many other faults the way may clear.
                List<String> meetings = laidOn.meetingsNotIn(world);
                if (joins && meetings.isEmpty() && faults.size() <= before.size()) {
                    return laid;
                }
                Set<String> seen = new HashSet<>(before);
                Optional<String> reason = !joins ? Optional.of("It does not join what it was to join")
                        : !meetings.isEmpty() ? Optional.of(meetings.getFirst()) : faults.stream().filter(fault -> !seen.contains(fault)).findFirst();
                tell(Step.ATTEMPT_REFUSED, pcb, Optional.of(line), laid.get().tracks().subList(pcb.tracks().size(), laid.get().tracks().size()), reason);
            }
        }
        return Optional.empty();
    }

    /** A connection this routing laid, and the copper it laid for it: what may be lifted to make room. */
    private record LaidRoute(CopperNets.Unlaid line, List<PcbDrawing.Track> tracks, List<PcbDrawing.Via> vias, List<PcbDrawing.Placement> links) {
    }

    /** What a layout gained over the one it was laid on: a laid connection only ever adds tracks, vias and links at the end. */
    private static LaidRoute routeOf(CopperNets.Unlaid line, PcbDrawing before, PcbDrawing after) {
        return new LaidRoute(line, after.tracks().subList(before.tracks().size(), after.tracks().size()),
                after.vias().subList(before.vias().size(), after.vias().size()),
                after.placements().subList(before.placements().size(), after.placements().size()));
    }

    /** The same things, by identity: a track the reader drew the same as a laid one is not the laid one. */
    private static <T> Set<T> identities(List<T> things) {
        Set<T> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(things);
        return set;
    }

    /** The layout without a route's copper. */
    private static PcbDrawing lifted(PcbDrawing pcb, LaidRoute route) {
        Set<PcbDrawing.Track> tracks = identities(route.tracks());
        Set<PcbDrawing.Via> vias = identities(route.vias());
        Set<PcbDrawing.Placement> links = identities(route.links());
        return pcb.withTracks(pcb.tracks().stream().filter(track -> !tracks.contains(track)).toList())
                .withVias(pcb.vias().stream().filter(via -> !vias.contains(via)).toList())
                .withPlacements(pcb.placements().stream().filter(placement -> !links.contains(placement)).toList());
    }

    private static List<Point> pointsOf(LaidRoute route) {
        List<Point> points = new ArrayList<>();
        route.tracks().forEach(track -> points.addAll(track.points()));
        route.vias().forEach(via -> points.add(new Point(via.x(), via.y())));
        return points;
    }

    /** Whether a route's copper comes within a margin of the box a connection spans: a route that may stand in its way. */
    private static boolean standsNear(LaidRoute route, CopperNets.Unlaid line, double margin) {
        double left = Math.min(line.from().x(), line.to().x()) - margin;
        double right = Math.max(line.from().x(), line.to().x()) + margin;
        double top = Math.min(line.from().y(), line.to().y()) - margin;
        double bottom = Math.max(line.from().y(), line.to().y()) + margin;
        return pointsOf(route).stream().anyMatch(point -> point.x() >= left && point.x() <= right && point.y() >= top && point.y() <= bottom);
    }

    /** How far a route stands from a connection's middle: the nearest is lifted first. */
    private static double distanceTo(LaidRoute route, CopperNets.Unlaid line) {
        Point middle = new Point((line.from().x() + line.to().x()) / 2, (line.from().y() + line.to().y()) / 2);
        return pointsOf(route).stream().mapToDouble(point -> Lines.lengthBetween(point, middle)).min().orElse(Double.POSITIVE_INFINITY);
    }

    /** Routing as it goes: the layout, the connections it laid, and those it could not. */
    private static final class RoutingState {
        private PcbDrawing               pcb;
        private List<LaidRoute>          routes;
        private List<CopperNets.Unlaid>  refused;

        RoutingState(PcbDrawing pcb, List<LaidRoute> routes, List<CopperNets.Unlaid> refused) {
            this.pcb = pcb;
            this.routes = routes;
            this.refused = refused;
        }
    }

    private void count(RoutingState state) {
        laidNow = state.routes.size();
        refusedNow = List.copyOf(state.refused);
    }

    /** One connection laid on the state's layout, its route kept; false where it could not be. */
    private boolean lay(RoutingState state, CopperNets.Unlaid line, List<String> layers) {
        tell(Step.TRYING, state.pcb, Optional.of(line));
        Optional<PcbDrawing> routed = laidConnection(state.pcb, line, layers);
        if (routed.isEmpty()) {
            return false;
        }
        state.routes.add(routeOf(line, state.pcb, routed.get()));
        state.pcb = routed.get();
        count(state);
        tell(Step.LAID, state.pcb, Optional.of(line));
        return true;
    }

    /** A piece of a route's copper and the layer it stands on; a via stands on all of them. */
    private record RouteCopper(CopperShape shape, Optional<String> layer) {
    }

    private List<RouteCopper> copperOf(LaidRoute route) {
        int curveSteps = board.checks().curveSteps();
        List<RouteCopper> copper = new ArrayList<>();
        route.tracks().forEach(track -> copper.add(new RouteCopper(CopperShapes.track(track, curveSteps), Optional.of(track.layer()))));
        route.vias().forEach(via -> copper.add(new RouteCopper(CopperShapes.via(via), Optional.empty())));
        return copper;
    }

    /**
     * The routes that stand on the way a connection would take if this routing had laid nothing: what has really to move for it.
     * Empty when it would find no way even then: lifting what this routing laid cannot give it one, so nothing is lifted at all.
     */
    private Optional<List<LaidRoute>> routesInItsWay(RoutingState state, CopperNets.Unlaid line, List<String> layers) {
        PcbDrawing bare = state.pcb;
        for (LaidRoute route : state.routes) {
            bare = lifted(bare, route);
        }
        Optional<PcbDrawing> seen = laidConnection(bare, line, layers);
        if (seen.isEmpty()) {
            return Optional.empty();
        }
        List<RouteCopper> way = copperOf(routeOf(line, bare, seen.get()));
        RoutingWorld world = worldOf(bare);
        List<LaidRoute> inTheWay = new ArrayList<>();
        for (LaidRoute route : state.routes) {
            if (route.line().net().equals(line.net())) {
                continue;
            }
            double gap = Math.max(world.clearanceOf(Optional.of(line.net())), world.clearanceOf(Optional.of(route.line().net())));
            boolean meets = copperOf(route).stream().anyMatch(copper -> way.stream().anyMatch(step ->
                    (step.layer().isEmpty() || copper.layer().isEmpty() || step.layer().equals(copper.layer()))
                    && !step.shape().keepsGap(copper.shape(), gap)));
            if (meets) {
                inTheWay.add(route);
            }
        }
        return Optional.of(inTheWay);
    }

    /** A connection laid with these routes lifted, then each of them laid again, the shortest first; kept only when all of them lie. */
    private boolean laidLifting(RoutingState state, CopperNets.Unlaid line, List<String> layers, List<LaidRoute> routes) {
        PcbDrawing liftedPcb = state.pcb;
        for (LaidRoute route : routes) {
            liftedPcb = lifted(liftedPcb, route);
        }
        Set<LaidRoute> liftedRoutes = identities(routes);
        RoutingState trial = new RoutingState(liftedPcb, new ArrayList<>(state.routes.stream().filter(kept -> !liftedRoutes.contains(kept)).toList()),
                state.refused);
        tell(Step.LIFTED, trial.pcb, Optional.of(line), routes.stream().flatMap(route -> route.tracks().stream()).toList(), Optional.empty());
        boolean all = lay(trial, line, layers);
        List<LaidRoute> again = new ArrayList<>(routes);
        again.sort(Comparator.comparingDouble(route -> lengthOf(route.line())));
        for (LaidRoute route : again) {
            all = all && lay(trial, route.line(), layers);
        }
        if (all) {
            state.pcb = trial.pcb;
            state.routes = trial.routes;
            return true;
        }
        count(state);
        tell(Step.RESTORED, state.pcb, Optional.of(line));
        return false;
    }

    /**
     * A refused connection retried: every route that really stands on its way lifted at once; failing that, one route near it at
     * a time. One that has no way even with all of this routing lifted is not retried: only the reader's copper stands in its way.
     */
    private boolean rippedUp(RoutingState state, CopperNets.Unlaid line, List<String> layers) {
        if (state.routes.isEmpty()) {
            return false;
        }
        Optional<List<LaidRoute>> inTheWay = routesInItsWay(state, line, layers);
        if (inTheWay.isEmpty()) {
            return false;
        }
        if (!inTheWay.get().isEmpty() && laidLifting(state, line, layers, inTheWay.get())) {
            return true;
        }
        RoutingSettings.RipUp ripUp = settings.ripUp();
        List<LaidRoute> near = state.routes.stream()
                .filter(route -> standsNear(route, line, ripUp.margin()) && !(inTheWay.get().size() == 1 && inTheWay.get().getFirst() == route))
                .sorted(Comparator.comparingDouble(route -> distanceTo(route, line))).limit(ripUp.candidates()).toList();
        for (LaidRoute route : near) {
            if (laidLifting(state, line, layers, List.of(route))) {
                return true;
            }
            if (outOfTime()) {
                return false;
            }
        }
        return false;
    }

    /** A connection's key: what says it is the same one in another round. */
    private static String keyOf(CopperNets.Unlaid line) {
        return line.net() + "|" + line.from() + "|" + line.to();
    }

    private record Round(RoutingState state, boolean stopped) {
    }

    /** One round: the connections laid, those in {@code first} before the rest, then each otherwise shortest first; the refused retried by ripping up. */
    private Round roundOf(PcbDrawing start, List<String> layers, Set<String> first) {
        RoutingState state = new RoutingState(start, new ArrayList<>(), new ArrayList<>());
        Set<String> refusedKeys = new HashSet<>();
        count(state);
        tell(Step.ROUND, state.pcb, Optional.empty());
        while (tried < settings.attempts()) {
            if (outOfTime()) {
                return new Round(state, true);
            }
            Optional<CopperNets.Unlaid> line = left(state.pcb).stream().filter(candidate -> !refusedKeys.contains(keyOf(candidate)))
                    .min(Comparator.<CopperNets.Unlaid>comparingInt(candidate -> first.contains(keyOf(candidate)) ? 0 : 1)
                            .thenComparingDouble(Autorouter::lengthOf));
            if (line.isEmpty()) {
                break;
            }
            tried += 1;
            if (!lay(state, line.get(), layers)) {
                refusedKeys.add(keyOf(line.get()));
                state.refused.add(line.get());
                count(state);
                tell(Step.REFUSED, state.pcb, line);
            }
        }
        for (int pass = 0; pass < settings.ripUp().passes() && !state.refused.isEmpty(); pass++) {
            List<CopperNets.Unlaid> waiting = state.refused;
            state.refused = new ArrayList<>();
            for (CopperNets.Unlaid line : waiting) {
                if (outOfTime()) {
                    return new Round(state, true);
                }
                if (!rippedUp(state, line, layers)) {
                    state.refused.add(line);
                    count(state);
                    tell(Step.REFUSED, state.pcb, Optional.of(line));
                }
                tried += 1;
            }
            if (state.refused.size() == waiting.size()) {
                break;
            }
        }
        return new Round(state, false);
    }

    /**
     * The pcb with every pad its net's plane can take dropped onto the plane first: what a plane joins is not laid again as
     * tracks. A drop is kept only where the checks find no more than they did without it; a pad whose copper reaches the plane by
     * the time the drops come to it is left as it is.
     */
    private PcbDrawing droppedOntoPlanes() {
        PcbDrawing pcb = request.pcb();
        RoutingWorld first = worldOf(pcb);
        List<String> keys = PlaneDrops.droppablePads(first, request.onlyNets()).stream()
                .map(droppable -> PlaneDrops.keyOf(first.nets().pads().get(droppable.pad()))).toList();
        for (String key : keys) {
            RoutingWorld world = worldOf(pcb);
            Optional<PlaneDrops.PlaneDrop> drop = PlaneDrops.droppablePads(world, request.onlyNets()).stream()
                    .filter(candidate -> PlaneDrops.keyOf(world.nets().pads().get(candidate.pad())).equals(key)).findFirst()
                    .flatMap(candidate -> PlaneDrops.planeDropOf(world, candidate));
            if (drop.isEmpty()) {
                continue;
            }
            List<PcbDrawing.Track> tracks = new ArrayList<>(pcb.tracks());
            tracks.add(drop.get().track().withPoints(drop.get().track().points().stream().map(this::written).toList()));
            List<PcbDrawing.Via> vias = new ArrayList<>(pcb.vias());
            vias.add(drop.get().via());
            PcbDrawing tried = pcb.withTracks(tracks).withVias(vias);
            if (worldOf(tried).faults().size() <= world.faults().size()) {
                pcb = tried;
            }
        }
        return pcb;
    }

    private Routed routed() {
        List<String> layers = request.pcb().copperLayerIds();
        PcbDrawing dropped = droppedOntoPlanes();
        total = left(dropped).size();
        round = 1;
        RoutingState best = null;
        Set<String> first = new HashSet<>();
        for (int at = 0; at < settings.rounds(); at++) {
            round = at + 1;
            Round done = roundOf(dropped, layers, first);
            RoutingState state = done.state();
            boolean better = best == null || left(state.pcb).size() < left(best.pcb).size();
            if (better) {
                best = state;
                laidBefore = state.routes.size();
            }
            if (done.stopped()) {
                return new Routed(best.pcb, best.routes.size(), left(best.pcb), true);
            }
            // Another order only helps a connection that something laid before it took the room of: with nothing laid, there is none.
            if (state.refused.isEmpty() || !better || state.routes.isEmpty()) {
                break;
            }
            state.refused.forEach(line -> first.add(keyOf(line)));
        }
        return new Routed(best.pcb, best.routes.size(), left(best.pcb), false);
    }
}
