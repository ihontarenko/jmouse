package org.jmouse.cad.routing.protocol;

import org.jmouse.cad.pcb.geometry.PadOutline;
import org.jmouse.cad.pcb.routing.RoutingBoard;
import org.jmouse.json.JsonMapper;

import java.util.Map;

/**
 * 🛤 A board as JSON: what the server worked out of a pcb's text — its footprints, nets, values, checks and settings — written so
 * a page can route on it without the store the text was read from. One mapper writes it and reads it back, so both sides agree
 * by construction. A pad outline, an object of two step counts, is written as those two numbers.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class RoutingBoardJson {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .serializer(PadOutline.class, (outline, output) -> output.object(members -> members
                    .member("curveSteps", outline.curveSteps())
                    .member("cornerSteps", outline.cornerSteps())))
            .deserializer(PadOutline.class, (json, deserialization) -> {
                Map<?, ?> steps = (Map<?, ?>) json;
                return new PadOutline(((Number) steps.get("curveSteps")).intValue(), ((Number) steps.get("cornerSteps")).intValue());
            })
            .build();

    private RoutingBoardJson() {
    }

    /** The mapper a board, and every gesture's request and answer, are read and written with. */
    public static JsonMapper mapper() {
        return MAPPER;
    }

    public static String write(RoutingBoard board) {
        return MAPPER.write(board);
    }

    public static RoutingBoard read(String json) {
        return MAPPER.read(json, RoutingBoard.class);
    }
}
