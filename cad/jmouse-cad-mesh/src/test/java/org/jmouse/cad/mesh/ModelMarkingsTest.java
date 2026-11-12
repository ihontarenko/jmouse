package org.jmouse.cad.mesh;

import org.jmouse.cad.document.DocumentLanguage;
import org.jmouse.cad.document.DocumentNode;
import org.jmouse.el.grammar.GrammarRefusal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A model says where its body is marked and with what, beside the shapes it is drawn from — every one of them drawn. */
class ModelMarkingsTest {

    private static final DocumentLanguage LANGUAGE = DocumentLanguage.of(new ModelModule());

    private static DocumentNode model(String lines) {
        return LANGUAGE.readOne("model:resistor-model(\"Resistor\") {\n" + lines + "\n}\n", "resistor-model.model.jm");
    }

    private static final String RESISTOR = """
            revolved(lacquer, profile: [0, 0, 1.2, 0, 1.25, 0.6, 1.1, 1.6, 1.1, 4.4, 1.25, 5.4, 1.2, 6, 0, 6], at: [0, 0, 1.25], rotate: [0, 90, 0])
            marking(bands, on: round, radius: 1.12, length: 2.8, at: [0, 0, 1.25], rotate: [0, 90, 0])
            marking(text, on: flat, size: [1, 1], at: [0, 0, 2.6], text: "+", ink: plastic)
            """;

    @Test
    void aTurnedBodyIsOneOfTheShapesItIsDrawnFrom() {
        List<ModelShapes.Shape> shapes = ModelShapes.of(model(RESISTOR));
        assertEquals(1, shapes.size());
        assertInstanceOf(ModelShapes.Revolved.class, shapes.getFirst());
        assertTrue(ModelShapes.mesh(shapes, ModelMaterials.standard()).triangles() > 0);
    }

    @Test
    void itsMarkingsAreReadAndCarriedAsTheBodysExtras() {
        List<ModelMarkings.Marking> markings = ModelMarkings.of(model(RESISTOR));
        assertEquals(2, markings.size());
        assertEquals(ModelMarkings.Shows.BANDS, markings.get(0).shows());
        assertEquals(new ModelMarkings.Round(1.12, 2.8, 0, 360), markings.get(0).surface());
        assertEquals("+", markings.get(1).text().orElseThrow());
        JsonNode extras = ModelMarkings.extrasOf(markings, ModelMaterials.standard());
        assertEquals("bands", extras.path("markings").path(0).path("shows").asString());
        assertEquals("round", extras.path("markings").path(0).path("surface").path("kind").asString());
        assertEquals(90, extras.path("markings").path(0).path("rotate").path(1).asDouble());
        assertEquals("flat", extras.path("markings").path(1).path("surface").path("kind").asString());
        assertEquals("+", extras.path("markings").path(1).path("text").asString());
        // The ink as the colour the materials table gives plastic: #1d1d1f.
        assertEquals("#1d1d1f", extras.path("markings").path(1).path("ink").asString());
        assertTrue(extras.path("markings").path(0).path("ink").isMissingNode());
    }

    @Test
    void aModelWithNoMarkingsSaysNothing() {
        assertNull(ModelMarkings.extrasOf(ModelMarkings.of(model("box(plastic, size: [1, 1, 1])")), ModelMaterials.standard()));
    }

    @Test
    void aMarkingIsRefusedWhatItsSurfaceCannotBe() {
        assertThrows(GrammarRefusal.class, () -> model("marking(bands, on: round, length: 2)"));
        assertThrows(GrammarRefusal.class, () -> model("marking(value, on: flat, size: [1, 2, 3], ink: plastic)"));
        assertThrows(GrammarRefusal.class, () -> model("marking(text, on: flat, size: [1, 1], ink: plastic)"));
        assertThrows(GrammarRefusal.class, () -> model("marking(value, on: flat, size: [1, 1])"));
        assertThrows(GrammarRefusal.class, () -> model("marking(bands, on: round, radius: 1, length: 2, ink: plastic)"));
        assertThrows(GrammarRefusal.class, () -> model("marking(value, on: flat, size: [1, 1], text: \"x\", ink: plastic)"));
    }
}
