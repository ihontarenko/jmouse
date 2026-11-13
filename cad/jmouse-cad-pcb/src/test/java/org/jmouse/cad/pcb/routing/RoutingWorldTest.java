package org.jmouse.cad.pcb.routing;

import org.jmouse.cad.pcb.checks.PcbChecks;
import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.geometry.PlacedDesignator;
import org.jmouse.cad.pcb.geometry.Teardrops;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a track keeps clear of: copper drawn as a shape is in the way of another net's track, and not of its own net's. */
class RoutingWorldTest {

    @Test
    void copperDrawnAsAShapeIsInTheWayOfATrackOnAnotherNetAndNotOfOneOnItsOwn() {
        PcbDrawing pcb = new PcbDrawing("circuit", Optional.empty(), List.of(), null, List.of(), List.of(), 2, null, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(new PcbDrawing.Shape(PcbDrawing.TOP, 0, true, "POWER", "M0 0H10V10H0Z")),
                List.of(), List.of());
        RoutingBoard board = new RoutingBoard(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), new PcbChecks.Settings(0.005, 6, 16,
                new PlacedDesignator.Rules(0.6, 0.9, 1.6, 0.25, 0.6), new Teardrops.Shape(8, 0.9, 0.8, 14, 24)), new PadOutline(16, 6), null);
        RoutingWorld world = new RoutingWorld(pcb, board);
        OptionalDouble none = OptionalDouble.of(0);
        assertEquals(1, world.obstacles(PcbDrawing.TOP, Optional.of("GROUND"), true, OptionalInt.empty(), none).size());
        assertEquals(0, world.obstacles(PcbDrawing.TOP, Optional.of("POWER"), true, OptionalInt.empty(), none).size());
        assertEquals(0, world.obstacles(PcbDrawing.BOTTOM, Optional.of("GROUND"), true, OptionalInt.empty(), none).size());
    }
}
