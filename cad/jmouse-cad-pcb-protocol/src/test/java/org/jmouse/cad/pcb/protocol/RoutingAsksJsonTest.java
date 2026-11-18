package org.jmouse.cad.pcb.protocol;

import org.jmouse.cad.pcb.drawing.PcbDrawing;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** A gesture's request as the editor sends it — the stand's pcb, as the editor wrote its layout — read by the protocol's mapper. */
class RoutingAsksJsonTest {

    private static String drawing() throws IOException {
        try (InputStream written = RoutingAsksJsonTest.class.getResourceAsStream("stand-tools-drawing.json")) {
            return new String(written.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void theEditorsDrawingIsReadIntoAPcbDrawing() throws IOException {
        PcbDrawing drawing = RoutingBoardJson.mapper().read(drawing(), PcbDrawing.class);

        assertEquals("stand-tools", drawing.circuit());
        assertFalse(drawing.placements().isEmpty());
    }

    @Test
    void aGesturesRequestIsReadWithItsDrawing() throws IOException {
        RoutingAsks.NodesRequest request = RoutingBoardJson.mapper().read("{\"drawing\": " + drawing() + "}", RoutingAsks.NodesRequest.class);

        assertEquals("stand-tools", request.drawing().circuit());
    }
}
