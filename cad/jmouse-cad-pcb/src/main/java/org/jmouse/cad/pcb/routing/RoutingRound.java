package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One round of a routing: the connections laid, those named first before the rest, then each otherwise shortest first; the refused
 * retried by lifting what was laid, pass after pass while a pass lays more.
 */
final class RoutingRound {

    /** A round done: how it stands, and whether it stopped before every connection was tried. */
    record Done(RoutingState state, boolean stopped) {
    }

    private final AutorouteSession session;
    private final RouteLifting     lifting;

    RoutingRound(AutorouteSession session, RouteLifting lifting) {
        this.session = session;
        this.lifting = lifting;
    }

    /** How long a connection is, end to end: the shortest is laid first. */
    static double lengthOf(CopperNets.Unlaid line) {
        return line.from().distanceTo(line.to());
    }

    /** A connection's key: what says it is the same one in another round. */
    static String keyOf(CopperNets.Unlaid line) {
        return line.net() + "|" + line.from() + "|" + line.to();
    }

    /** A round from a layout, the connections keyed in {@code first} laid before the rest. */
    Done of(PcbDrawing start, List<String> layers, Set<String> first) {
        RoutingState state = new RoutingState(start, new ArrayList<>(), new ArrayList<>());
        Set<String> refusedKeys = new HashSet<>();
        session.counted(state.routes.size(), state.refused);
        session.tell(Autorouter.Step.ROUND, state.pcb, Optional.empty());
        while (session.tried() < session.settings().attempts()) {
            if (session.outOfTime()) {
                return new Done(state, true);
            }
            Optional<CopperNets.Unlaid> line = session.left(state.pcb).stream().filter(candidate -> !refusedKeys.contains(keyOf(candidate)))
                    .min(Comparator.<CopperNets.Unlaid>comparingInt(candidate -> first.contains(keyOf(candidate)) ? 0 : 1)
                            .thenComparingDouble(RoutingRound::lengthOf));
            if (line.isEmpty()) {
                break;
            }
            session.triedOne();
            if (!lifting.lay(state, line.get(), layers)) {
                refusedKeys.add(keyOf(line.get()));
                state.refused.add(line.get());
                session.counted(state.routes.size(), state.refused);
                session.tell(Autorouter.Step.REFUSED, state.pcb, line);
            }
        }
        for (int pass = 0; pass < session.settings().ripUp().passes() && !state.refused.isEmpty(); pass++) {
            List<CopperNets.Unlaid> waiting = state.refused;
            state.refused = new ArrayList<>();
            for (CopperNets.Unlaid line : waiting) {
                if (session.outOfTime()) {
                    return new Done(state, true);
                }
                if (!lifting.rippedUp(state, line, layers)) {
                    state.refused.add(line);
                    session.counted(state.routes.size(), state.refused);
                    session.tell(Autorouter.Step.REFUSED, state.pcb, Optional.of(line));
                }
                session.triedOne();
            }
            if (state.refused.size() == waiting.size()) {
                break;
            }
        }
        return new Done(state, false);
    }
}
