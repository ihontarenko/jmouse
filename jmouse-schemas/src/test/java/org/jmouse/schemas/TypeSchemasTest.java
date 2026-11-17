package org.jmouse.schemas;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeSchemasTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /**
     * A drawing to try the writer on.
     *
     * @param title    what it is called
     * @param shapes   what it draws
     * @param side     where it stands
     * @param note     what is said of it, where anything is
     * @param measures named numbers
     * @param inside   drawings inside it
     */
    record Drawing(String title, List<Shape> shapes, Side side, Optional<String> note, Map<String, Double> measures, List<Drawing> inside) {
    }

    /** A shape of a drawing. */
    sealed interface Shape permits Circle, Line {
    }

    /**
     * A round shape.
     *
     * @param radius how far its edge is
     */
    record Circle(double radius) implements Shape {
    }

    /**
     * A straight shape.
     *
     * @param length how long it is
     */
    record Line(double length) implements Shape {
    }

    /** Which side a drawing stands on. */
    enum Side {
        TOP, BOTTOM
    }

    /** Undescribed on purpose. */
    record Bare(int count) {
    }

    @Test
    void aModelsOwnJsonHoldsToItsSchema() {
        WrittenSchema written = TypeSchemas.write(Drawing.class, SchemaStyle.defined());
        assertEquals(List.of(), written.undescribed());
        Drawing drawing = new Drawing("Board", List.of(new Circle(2), new Line(5)), Side.TOP, Optional.empty(), Map.of("width", 10.0),
                                      List.of(new Drawing("Inner", List.of(), Side.BOTTOM, Optional.of("small"), Map.of(), List.of())));
        JsonNode value = JSON.valueToTree(drawing);
        assertEquals(List.of(), SchemaChecks.problemsOf(written.schema(), value));
    }

    @Test
    void sayEachTypeOnceUnderItsNameInItsPackage() {
        ObjectNode schema = TypeSchemas.write(Drawing.class, SchemaStyle.defined()).schema();
        JsonNode defined = schema.get("$defs");
        assertTrue(defined.has("TypeSchemasTest.Circle"), schema.toPrettyString());
        assertEquals("A round shape.", defined.get("TypeSchemasTest.Circle").get("description").asString());
        assertEquals("how far its edge is", defined.get("TypeSchemasTest.Circle").get("properties").get("radius").get("description").asString());
        assertTrue(schema.get("required").toString().contains("note"), "an empty value is written as null, so it is always there");
        assertEquals("[\"string\",\"null\"]", schema.get("properties").get("note").get("type").toString());
        ObjectNode leftOut = TypeSchemas.write(Drawing.class, SchemaStyle.defined().absent(Absence.LEFT_OUT)).schema();
        assertFalse(leftOut.get("required").toString().contains("note"));
    }

    /**
     * A pad, with what it says of itself beside its components.
     *
     * @param drill how wide its hole is
     * @param name  what it is called, where it is called anything
     */
    record Pad(double drill, @Nullable String name) {

        /** Whether it has no hole. */
        public boolean isSurface() {
            return drill == 0;
        }
    }

    @Test
    void aGetterIsAPropertyAndANullableTypeMayBeNull() {
        WrittenSchema written = TypeSchemas.write(Pad.class, SchemaStyle.defined());
        assertEquals(List.of(), written.undescribed());
        JsonNode properties = written.schema().get("properties");
        assertEquals("Whether it has no hole.", properties.get("surface").get("description").asString());
        assertEquals("[\"string\",\"null\"]", properties.get("name").get("type").toString());
        assertEquals(List.of(), SchemaChecks.problemsOf(written.schema(), JSON.valueToTree(new Pad(0, null))));
    }

    @Test
    void refusesWhatTheSchemaDoesNotAllow() {
        ObjectNode schema = TypeSchemas.write(Drawing.class, SchemaStyle.defined()).schema();
        ObjectNode value = JSON.valueToTree(new Drawing("Board", List.of(), Side.TOP, Optional.empty(), Map.of(), List.of()));
        value.put("side", "LEFT");
        value.put("colour", "red");
        assertEquals(2, SchemaChecks.problemsOf(schema, value).size());
    }

    @Test
    void notesWhatItsJavadocDoesNotSay() {
        List<String> undescribed = TypeSchemas.write(Bare.class, SchemaStyle.defined()).undescribed();
        assertEquals(List.of("TypeSchemasTest.Bare.count: its record's Javadoc has no @param count"), undescribed);
    }
}
