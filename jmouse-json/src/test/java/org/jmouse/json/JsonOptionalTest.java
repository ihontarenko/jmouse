package org.jmouse.json;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A record with optional, enum, map and nested components read as Jackson writes them: an absent optional as null or not at all. */
class JsonOptionalTest {

    public enum Arrangement {
        FREE, LOCKED
    }

    public record At(double x, double y) {
    }

    public record Placed(String name, Optional<At> at, Optional<String> note, Arrangement arrangement, Map<String, Double> values, List<At> path) {
    }

    @Test
    void anOptionalIsReadFromItsValueOrFromNull() {
        Placed placed = Json.read("""
                {"name": "R1", "at": {"x": 1, "y": 2}, "note": null, "arrangement": "LOCKED", "values": {"r": 4.7}, "path": [{"x": 0, "y": 0}]}
                """, Placed.class);

        assertEquals(new Placed("R1", Optional.of(new At(1, 2)), Optional.empty(), Arrangement.LOCKED, Map.of("r", 4.7), List.of(new At(0, 0))), placed);
    }

    @Test
    void anOptionalLeftOutIsEmpty() {
        Placed placed = Json.read("{\"name\": \"R2\", \"arrangement\": \"FREE\", \"values\": {}, \"path\": []}", Placed.class);

        assertEquals(Optional.empty(), placed.at());
        assertEquals(Optional.empty(), placed.note());
    }

    public record Named(Map<String, Optional<String>> footprints) {
    }

    @Test
    void anOptionalAsAMapsValueIsEmptyWhereItHeldNull() {
        Named named = Json.read("{\"footprints\": {\"R1\": \"R0805\", \"X1\": null}}", Named.class);

        assertEquals(Optional.of("R0805"), named.footprints().get("R1"));
        assertEquals(Optional.empty(), named.footprints().get("X1"));
    }
}
