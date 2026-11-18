package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.PcbVocabulary;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One connection laid: its way kept only where the checks find nothing new in it and it truly joins what it was to join — its net's
 * width tried first, then the least the pcb is made with, and a 0 Ω link last where the request allows one.
 */
final class ConnectionLaying {

    private final AutorouteSession session;
    private final ConnectionWay    way;

    ConnectionLaying(AutorouteSession session) {
        this.session = session;
        this.way = new ConnectionWay(session);
    }

    /** The layout with the connection laid on it; empty where no way it finds passes the checks. */
    Optional<PcbDrawing> laid(PcbDrawing pcb, CopperNets.Unlaid line, List<String> layers) {
        RoutingWorld world = session.worldOf(pcb);
        List<String> before = world.faults();
        int unjoined = world.unjoined();
        // Its net's width first, then thin all the way, as a person gives up a width before a link: what fits with least given up.
        double width = world.trackWidthOf(Optional.of(line.net()));
        double least = world.rules().value(PcbVocabulary.RULE_LEAST_TRACK_WIDTH);
        List<Double> widths = least > 0 && least < width ? List.of(width, least) : List.of(width);
        List<Boolean> linkings = session.request().crossing() == Autorouter.Crossing.LINKS ? List.of(false, true) : List.of(false);
        for (boolean linking : linkings) {
            for (double tried : widths) {
                Optional<PcbDrawing> laid = way.laid(world, line, layers, linking, tried);
                if (laid.isEmpty()) {
                    session.tell(Autorouter.Step.ATTEMPT_REFUSED, pcb, Optional.of(line), List.of(), Optional.of("No way found"));
                    continue;
                }
                RoutingWorld laidOn = session.worldOf(laid.get());
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
                session.tell(Autorouter.Step.ATTEMPT_REFUSED, pcb, Optional.of(line),
                        laid.get().tracks().subList(pcb.tracks().size(), laid.get().tracks().size()), reason);
            }
        }
        return Optional.empty();
    }
}
