package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.drawing.Placement;
import org.jmouse.cad.pcb.checks.PcbChecks;
import org.jmouse.cad.pcb.drawing.FootprintDrawing;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.PadOutline;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * What the router knows of a pcb besides where its copper runs: each part's footprint and the nets its pads are on, as the
 * circuit wires them, the values its rules give, and how the engine measures. Read once for a pcb; every layout of it the
 * router is asked about — a drag's every frame, a routing's every step — stands on it.
 *
 * @param footprints    each seated part's footprint, by designator
 * @param named         for each placement, the footprint its part names; empty where it names none
 * @param partNets      each part's pads and the net each is on, by designator, then pad number
 * @param values        the values the pcb is held to, by rule word
 * @param ownFootprints the footprints a part the pcb places itself may stand on, by slug — a 0 Ω link's among them
 * @param checks        how the checks measure, which a routed connection is held to
 * @param outlines      how a pad's outline is drawn as a polygon
 */
public record RoutingBoard(Map<String, FootprintDrawing> footprints, Map<String, Optional<String>> named,
                           Map<String, Map<String, String>> partNets, Map<String, Double> values, Map<String, FootprintDrawing> ownFootprints,
                           PcbChecks.Settings checks, PadOutline outlines, RoutingSettings settings) {

    public RoutingBoard {
        footprints = Map.copyOf(footprints);
        named = Map.copyOf(named);
        partNets = Map.copyOf(partNets);
        values = Map.copyOf(values);
        ownFootprints = Map.copyOf(ownFootprints);
    }

    /** The footprint a 0 Ω link stands on; empty where it was not read. */
    public Optional<FootprintDrawing> linkFootprint() {
        return Optional.ofNullable(ownFootprints.get(settings.autorouting().link().footprint()));
    }

    /** Each part's footprint on a layout of the pcb: the seated parts', and what the layout places itself on a footprint read. */
    public Map<String, FootprintDrawing> footprintsOn(PcbDrawing pcb) {
        Map<String, FootprintDrawing> seated = new LinkedHashMap<>(footprints);
        for (Placement placement : pcb.placements()) {
            if (!seated.containsKey(placement.designator()) && placement.ownedByPcb()) {
                placement.footprint().map(ownFootprints::get).ifPresent(drawing -> seated.put(placement.designator(), drawing));
            }
        }
        return seated;
    }

    /** Each placement's named footprint on a layout: what the layout places itself names its own. */
    public Map<String, Optional<String>> namedOn(PcbDrawing pcb) {
        Map<String, Optional<String>> seated = new LinkedHashMap<>(named);
        for (Placement placement : pcb.placements()) {
            if (!seated.containsKey(placement.designator()) && placement.ownedByPcb()) {
                seated.put(placement.designator(), placement.footprint());
            }
        }
        return seated;
    }

    /** Each part's pads and their nets on a layout: what the layout places itself is on the nets it says. */
    public Map<String, Map<String, String>> partNetsOn(PcbDrawing pcb) {
        Map<String, Map<String, String>> seated = new LinkedHashMap<>(partNets);
        for (Placement placement : pcb.placements()) {
            if (!seated.containsKey(placement.designator()) && placement.ownedByPcb()) {
                seated.put(placement.designator(), placement.nets());
            }
        }
        return seated;
    }
}
