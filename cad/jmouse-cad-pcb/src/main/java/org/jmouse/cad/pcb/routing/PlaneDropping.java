package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.Track;
import org.jmouse.cad.pcb.drawing.Via;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Every pad its net's plane can take dropped onto the plane before anything is routed: what a plane joins is not laid again as
 * tracks. A drop is kept only where the checks find no more than they did without it; a pad whose copper reaches the plane by the time
 * the drops come to it is left as it is.
 */
final class PlaneDropping {

    private final AutorouteSession session;

    PlaneDropping(AutorouteSession session) {
        this.session = session;
    }

    /** The asked layout with its pads dropped onto their planes. */
    PcbDrawing dropped() {
        PcbDrawing pcb = session.request().pcb();
        RoutingWorld first = session.worldOf(pcb);
        List<String> keys = PlaneDrops.droppablePads(first, session.request().onlyNets()).stream()
                .map(droppable -> PlaneDrops.keyOf(first.nets().pads().get(droppable.pad()))).toList();
        for (String key : keys) {
            RoutingWorld world = session.worldOf(pcb);
            Optional<PlaneDrops.PlaneDrop> drop = PlaneDrops.droppablePads(world, session.request().onlyNets()).stream()
                    .filter(candidate -> PlaneDrops.keyOf(world.nets().pads().get(candidate.pad())).equals(key)).findFirst()
                    .flatMap(candidate -> PlaneDrops.planeDropOf(world, candidate));
            if (drop.isEmpty()) {
                continue;
            }
            List<Track> tracks = new ArrayList<>(pcb.tracks());
            tracks.add(drop.get().track().withPoints(drop.get().track().points().stream().map(session::written).toList()));
            List<Via> vias = new ArrayList<>(pcb.vias());
            vias.add(drop.get().via());
            PcbDrawing tried = pcb.withTracks(tracks).withVias(vias);
            if (session.worldOf(tried).faults().size() <= world.faults().size()) {
                pcb = tried;
            }
        }
        return pcb;
    }
}
