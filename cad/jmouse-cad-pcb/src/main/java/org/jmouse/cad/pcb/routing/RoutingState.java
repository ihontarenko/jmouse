package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.CopperNets;

import java.util.List;

/** A routing as it goes: the layout, the connections it laid, and those it could not. */
final class RoutingState {

    PcbDrawing              pcb;
    List<LaidRoute>         routes;
    List<CopperNets.Unlaid> refused;

    RoutingState(PcbDrawing pcb, List<LaidRoute> routes, List<CopperNets.Unlaid> refused) {
        this.pcb = pcb;
        this.routes = routes;
        this.refused = refused;
    }
}
