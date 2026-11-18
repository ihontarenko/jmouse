package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A refused connection retried by lifting what the routing laid: every route that really stands on its way at once; failing that, one
 * route near it at a time. Kept only when the connection and every route lifted for it lie again; a track the reader drew is never
 * lifted.
 */
final class RouteLifting {

    private final AutorouteSession session;
    private final ConnectionLaying laying;

    RouteLifting(AutorouteSession session, ConnectionLaying laying) {
        this.session = session;
        this.laying = laying;
    }

    /** One connection laid on the state's layout, its route kept; false where it could not be. */
    boolean lay(RoutingState state, CopperNets.Unlaid line, List<String> layers) {
        session.tell(Autorouter.Step.TRYING, state.pcb, Optional.of(line));
        Optional<PcbDrawing> routed = laying.laid(state.pcb, line, layers);
        if (routed.isEmpty()) {
            return false;
        }
        state.routes.add(LaidRoute.of(line, state.pcb, routed.get()));
        state.pcb = routed.get();
        session.counted(state.routes.size(), state.refused);
        session.tell(Autorouter.Step.LAID, state.pcb, Optional.of(line));
        return true;
    }

    /**
     * A refused connection retried: every route that really stands on its way lifted at once; failing that, one route near it at a
     * time. One that has no way even with all of this routing lifted is not retried: only the reader's copper stands in its way.
     */
    boolean rippedUp(RoutingState state, CopperNets.Unlaid line, List<String> layers) {
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
        RoutingSettings.RipUp ripUp = session.settings().ripUp();
        List<LaidRoute> near = state.routes.stream()
                .filter(route -> route.standsNear(line, ripUp.margin()) && !(inTheWay.get().size() == 1 && inTheWay.get().getFirst() == route))
                .sorted(Comparator.comparingDouble(route -> route.distanceTo(line))).limit(ripUp.candidates()).toList();
        for (LaidRoute route : near) {
            if (laidLifting(state, line, layers, List.of(route))) {
                return true;
            }
            if (session.outOfTime()) {
                return false;
            }
        }
        return false;
    }

    /**
     * The routes that stand on the way a connection would take if this routing had laid nothing: what has really to move for it.
     * Empty when it would find no way even then: lifting what this routing laid cannot give it one, so nothing is lifted at all.
     */
    private Optional<List<LaidRoute>> routesInItsWay(RoutingState state, CopperNets.Unlaid line, List<String> layers) {
        PcbDrawing bare = state.pcb;
        for (LaidRoute route : state.routes) {
            bare = route.liftedFrom(bare);
        }
        Optional<PcbDrawing> seen = laying.laid(bare, line, layers);
        if (seen.isEmpty()) {
            return Optional.empty();
        }
        int curveSteps = session.board().checks().curveSteps();
        List<LaidRoute.Copper> way = LaidRoute.of(line, bare, seen.get()).copper(curveSteps);
        RoutingWorld world = session.worldOf(bare);
        List<LaidRoute> inTheWay = new ArrayList<>();
        for (LaidRoute route : state.routes) {
            if (route.line().net().equals(line.net())) {
                continue;
            }
            double gap = Math.max(world.clearanceOf(Optional.of(line.net())), world.clearanceOf(Optional.of(route.line().net())));
            boolean meets = route.copper(curveSteps).stream().anyMatch(copper -> way.stream().anyMatch(step ->
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
            liftedPcb = route.liftedFrom(liftedPcb);
        }
        Set<LaidRoute> liftedRoutes = LaidRoute.identities(routes);
        RoutingState trial = new RoutingState(liftedPcb, new ArrayList<>(state.routes.stream().filter(kept -> !liftedRoutes.contains(kept)).toList()),
                state.refused);
        session.tell(Autorouter.Step.LIFTED, trial.pcb, Optional.of(line), routes.stream().flatMap(route -> route.tracks().stream()).toList(),
                Optional.empty());
        boolean all = lay(trial, line, layers);
        List<LaidRoute> again = new ArrayList<>(routes);
        again.sort(Comparator.comparingDouble(route -> RoutingRound.lengthOf(route.line())));
        for (LaidRoute route : again) {
            all = all && lay(trial, route.line(), layers);
        }
        if (all) {
            state.pcb = trial.pcb;
            state.routes = trial.routes;
            return true;
        }
        session.counted(state.routes.size(), state.refused);
        session.tell(Autorouter.Step.RESTORED, state.pcb, Optional.of(line));
        return false;
    }
}
