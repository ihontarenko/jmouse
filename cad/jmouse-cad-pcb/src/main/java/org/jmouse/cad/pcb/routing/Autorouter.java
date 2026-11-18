package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.geometry.CopperNets;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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
    public record Moment(Step step, PcbDrawing pcb, Optional<CopperNets.Unlaid> line, List<Track> attempt, Optional<String> reason,
                         List<CopperNets.Unlaid> refused, int laid, int tried, int total, int round) {
    }

    /**
     * @param laid     how many connections were laid
     * @param unrouted the connections still unrouted: those no layer and no via could carry within the rules
     * @param stopped  whether it stopped because it was asked to, before every connection was tried
     */
    public record Routed(PcbDrawing pcb, int laid, List<CopperNets.Unlaid> unrouted, boolean stopped) {
    }

    private Autorouter() {
    }

    /**
     * The pcb with its unrouted connections laid, on every layer it has, each within the rules: shortest first, the refused
     * retried by ripping up what this routing laid near them. What is left is tried again from the start, it going first, for as
     * long as that lays more. Tracks take their net's width, vias their net's size, as a track drawn by hand.
     */
    public static Routed route(RoutingBoard board, Request request) {
        AutorouteSession session = new AutorouteSession(board, request);
        RoutingRound rounds = new RoutingRound(session, new RouteLifting(session, new ConnectionLaying(session)));
        List<String> layers = request.pcb().copperLayerIds();
        PcbDrawing dropped = new PlaneDropping(session).dropped();
        session.totalOf(session.left(dropped).size());
        session.inRound(1);
        RoutingState best = null;
        Set<String> first = new HashSet<>();
        for (int at = 0; at < session.settings().rounds(); at++) {
            session.inRound(at + 1);
            RoutingRound.Done done = rounds.of(dropped, layers, first);
            RoutingState state = done.state();
            boolean better = best == null || session.left(state.pcb).size() < session.left(best.pcb).size();
            if (better) {
                best = state;
                session.bestLaid(state.routes.size());
            }
            if (done.stopped()) {
                return new Routed(best.pcb, best.routes.size(), session.left(best.pcb), true);
            }
            // Another order only helps a connection that something laid before it took the room of: with nothing laid, there is none.
            if (state.refused.isEmpty() || !better || state.routes.isEmpty()) {
                break;
            }
            state.refused.forEach(line -> first.add(RoutingRound.keyOf(line)));
        }
        return new Routed(best.pcb, best.routes.size(), session.left(best.pcb), false);
    }
}
