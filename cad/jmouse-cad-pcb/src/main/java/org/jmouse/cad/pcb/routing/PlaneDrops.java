package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing.Point;
import org.jmouse.cad.pcb.geometry.CopperNets;
import org.jmouse.cad.pcb.geometry.CopperShape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A pad dropped onto its net's plane, as a person joins a surface pad to a ground plane on the other side: a short track of its
 * net's width out of the pad to a through via standing in the plane, as near as the gaps allow. The plane then joins the pad,
 * and what the plane joins is not laid again as tracks.
 */
public final class PlaneDrops {

    private PlaneDrops() {
    }

    /** A drop: the track out of its pad and the via at its far end. */
    public record PlaneDrop(PcbDrawing.Track track, PcbDrawing.Via via) {
    }

    /**
     * A pad that a plane could take: of the plane's net, inside its outline, on a layer the plane is not poured on.
     *
     * @param pad   its place among the pcb's pads
     * @param plane the plane's outline, as copper: where the drop's via has to stand
     */
    public record DroppablePad(int pad, String net, CopperShape plane) {
    }

    /** What names a pad from one layout to the next: its part and its number. */
    public static String keyOf(CopperNets.PlacedPad pad) {
        return pad.designator() + "|" + pad.pad().number();
    }

    /**
     * The pads of a board that their net's plane could join and nothing joins to it yet: each surface pad of a zone's net, inside
     * that zone, on a layer the zone is not poured on, whose copper reaches no pad or via standing in the zone.
     *
     * @param nets the nets dropped; empty for every one
     */
    public static List<DroppablePad> droppablePads(RoutingWorld world, Set<String> nets) {
        PcbDrawing pcb = world.pcb();
        CopperNets copper = world.nets();
        int curveSteps = world.board().checks().curveSteps();
        List<DroppablePad> found = new ArrayList<>();
        for (PcbDrawing.Zone zone : pcb.zones()) {
            if (!nets.isEmpty() && !nets.contains(zone.net())) {
                continue;
            }
            Optional<CopperShape> outline = CopperShape.ofPath(zone.path(), true, 0, curveSteps);
            if (outline.isEmpty()) {
                continue;
            }
            CopperShape plane = outline.get();
            Optional<String> net = Optional.of(zone.net());
            Set<Integer> onPlane = new HashSet<>();
            for (int index = 0; index < copper.pads().size(); index++) {
                CopperNets.PlacedPad pad = copper.pads().get(index);
                if (copper.padNets().get(index).equals(net) && pad.on(zone.layer()) && plane.encloses(pad.centre().x(), pad.centre().y())) {
                    onPlane.add(copper.padGroup(index));
                }
            }
            for (int index = 0; index < pcb.vias().size(); index++) {
                PcbDrawing.Via via = pcb.vias().get(index);
                if (copper.viaNets().get(index).equals(net) && plane.encloses(via.x(), via.y())) {
                    onPlane.add(copper.viaGroup(index));
                }
            }
            for (int index = 0; index < copper.pads().size(); index++) {
                CopperNets.PlacedPad pad = copper.pads().get(index);
                if (copper.padNets().get(index).equals(net) && !pad.on(zone.layer()) && plane.encloses(pad.centre().x(), pad.centre().y())
                    && !onPlane.contains(copper.padGroup(index))) {
                    found.add(new DroppablePad(index, zone.net(), plane));
                }
            }
        }
        return found;
    }

    /** The one surface layer a pad has copper on; empty for a pad through every layer. */
    private static Optional<String> surfaceOf(RoutingWorld world, CopperNets.PlacedPad pad) {
        List<String> layers = world.pcb().copperLayerIds().stream().filter(pad::on).toList();
        return layers.size() == 1 ? Optional.of(layers.getFirst()) : Optional.empty();
    }

    /**
     * The nearest drop a pad has into its plane: the via, of its net's size, the first place out of the pad along any of the
     * eight ways where it keeps its gap from the pad and from everything on every layer, inside the plane, the track to it clear
     * on the pad's layer. Empty where none stands within the settings' reach.
     */
    public static Optional<PlaneDrop> planeDropOf(RoutingWorld world, DroppablePad droppable) {
        CopperNets.PlacedPad pad = world.nets().pads().get(droppable.pad());
        Optional<String> surface = surfaceOf(world, pad);
        if (surface.isEmpty()) {
            return Optional.empty();
        }
        String layer = surface.get();
        Optional<String> net = Optional.of(droppable.net());
        PcbDrawing.ViaSize via = world.rules().viaOf(net);
        double width = world.trackWidthOf(net);
        double clearance = world.clearanceOf(net);
        TrackFence track = world.fence(layer, net, width);
        List<TrackFence> viaFences = world.pcb().copperLayerIds().stream().map(each -> world.fence(each, net, via.diameter())).toList();
        RoutingSettings.PlaneDrops settings = world.settings().autorouting().planeDrops();
        Point from = pad.centre();
        Point best = null;
        double shortest = Double.POSITIVE_INFINITY;
        for (Point way : Lines.STRAIGHT_FIRST_WAYS) {
            for (double out = settings.step(); out <= settings.reach() && out < shortest; out += settings.step()) {
                Point at = Lines.settled(Lines.offset(from, way, out));
                if (!track.lineClear(List.of(from, at))) {
                    break;
                }
                boolean clearOfPad = pad.copper().keepsGap(CopperShape.circle(at.x(), at.y(), via.diameter() / 2), clearance);
                if (clearOfPad && droppable.plane().encloses(at.x(), at.y()) && viaFences.stream().allMatch(fence -> fence.pointClear(at))) {
                    best = at;
                    shortest = out;
                    break;
                }
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        return Optional.of(new PlaneDrop(new PcbDrawing.Track(layer, width, 0, List.of(from, best)),
                new PcbDrawing.Via(best.x(), best.y(), via.drill(), via.diameter(), PcbDrawing.TOP, PcbDrawing.BOTTOM)));
    }
}
