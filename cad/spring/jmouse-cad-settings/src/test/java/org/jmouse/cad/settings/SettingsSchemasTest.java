package org.jmouse.cad.settings;

import org.jmouse.settings.Label;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A section's schema written from its record and the record's Javadoc, and what cannot be written said as a mismatch. */
class SettingsSchemasTest {

    /**
     * How a sample track is drawn: as {@code sample/track} says.
     *
     * @param width      how wide
     * @param style      how its line is drawn
     * @param note       what a reader wrote of it, if anything
     * @param layers     the layers it may cross
     * @param clearances each class's clearance, by its name
     */
    record Track(@Label(value = "Width", unit = "mm") double width, @Label("Style") Style style, @Label("Note") Optional<String> note,
                 @Label("Layers") List<String> layers, @Label("Clearances") Map<String, Clearance> clearances, int silent) {
    }

    /** How a line is drawn. */
    enum Style {
        @JsonProperty("solid") SOLID,
        @JsonProperty("dashed") DASHED
    }

    /**
     * A clearance.
     *
     * @param gap the gap kept
     */
    @Label("Clearance")
    record Clearance(@Label(value = "Gap", unit = "mm") double gap) {
    }

    private static final SettingsSection<Track> SECTION = new SettingsSection<>("sample/track", "Sample track",
            Track.class);

    @Test
    void writesTheSchemaFromTheRecordAndItsJavadoc() {
        JsonNode schema = SettingsSchemas.of(SECTION);
        assertEquals("Sample track", schema.path("title").asString());
        assertEquals("How a sample track is drawn: as sample/track says.", schema.path("description").asString());
        JsonNode properties = schema.path("properties");
        assertEquals("how wide", properties.path("width").path("description").asString());
        assertEquals("Width", properties.path("width").path("title").asString());
        assertEquals("mm", properties.path("width").path("unit").asString());
        assertEquals("number", properties.path("width").path("type").asString());
        assertEquals(List.of("solid", "dashed"), properties.path("style").path("enum").valueStream().map(JsonNode::asString).toList());
        assertEquals(List.of("string", "null"), properties.path("note").path("type").valueStream().map(JsonNode::asString).toList());
        assertEquals("string", properties.path("layers").path("items").path("type").asString());
        assertEquals("Clearance", properties.path("clearances").path("additionalProperties").path("title").asString());
        assertEquals("the gap kept", properties.path("clearances").path("additionalProperties").path("properties").path("gap")
                .path("description").asString());
        assertEquals(List.of("width", "style", "layers", "clearances", "silent"),
                schema.path("required").valueStream().map(JsonNode::asString).toList());
    }

    @Test
    void saysWhatItCannotDescribe() {
        assertEquals(List.of("sample/track.silent: org.jmouse.cad.settings.SettingsSchemasTest$Track's component silent has no @Label: a reader is never shown a key",
                             "sample/track.silent: Track's Javadoc has no @param silent"), SettingsSchemas.mismatches(SECTION));
    }
}
