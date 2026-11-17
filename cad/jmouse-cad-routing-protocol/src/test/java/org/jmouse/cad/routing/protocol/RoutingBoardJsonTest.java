package org.jmouse.cad.routing.protocol;

import org.jmouse.cad.pcb.routing.RoutingBoard;
import org.jmouse.cad.pcb.routing.RoutingEngine;
import org.jmouse.json.Json;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A board as the server writes it, read back: the same board — written again it is the same text —, and the editor's own leg laid
 * on it as the server laid it.
 */
class RoutingBoardJsonTest {

    private static String resource(String name) throws IOException {
        try (InputStream written = RoutingBoardJsonTest.class.getResourceAsStream(name)) {
            return new String(written.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void aBoardReadBackIsWrittenAsItWas() throws IOException {
        String written = resource("stand-tools-board.json");

        // Compared as values: the board's maps have no order of their own, so neither has their text.
        assertEquals(Json.read(written), Json.read(RoutingBoardJson.write(RoutingBoardJson.read(written))));
    }

    @Test
    void theEditorsLegIsLaidOnABoardReadFromJsonAsTheServerLaidIt() throws IOException {
        RoutingBoard board = RoutingBoardJson.read(resource("stand-tools-board.json"));
        RoutingAsks.LegRequest leg = RoutingBoardJson.mapper().read(resource("stand-tools-leg.json"), RoutingAsks.LegRequest.class);

        RoutingAsks.LegView laid = RoutingAsks.leg(new RoutingEngine(board), leg);

        // As the server answered the same request, its board worked out of the pcb's text.
        assertEquals("{\"points\":[{\"x\":49.425,\"y\":10},{\"x\":58.05,\"y\":18.625},{\"x\":58.05,\"y\":19.05},{\"x\":62.865,\"y\":19.05}],\"reached\":true,\"rounding\":1}",
                     RoutingBoardJson.mapper().write(laid));
    }
}
