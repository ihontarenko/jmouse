package org.jmouse.mesh.gltf;

import org.jmouse.mesh.MeshBuilder;
import org.jmouse.mesh.MeshMaterial;
import org.jmouse.mesh.MeshScene;
import org.jmouse.mesh.Placement;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** What a scene says of itself rides on its root as glTF's extras, and a scene that says nothing writes none. */
class GlbWriterTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final MeshMaterial BODY = new MeshMaterial("body", 0.5, 0.5, 0.5, 1, 0, 0.5);

    private static JsonNode rootOf(byte[] glb) {
        return JSON.readTree(GlbContainer.read(glb, "written").json()).path("nodes").path(0);
    }

    @Test
    void aScenesExtrasStandOnItsRoot() {
        ObjectNode said = JSON.createObjectNode();
        said.putArray("markings").addObject().put("shows", "bands").put("radius", 1.2);
        MeshScene scene = MeshScene.of(new MeshBuilder().box(BODY, Placement.IDENTITY, 1, 1, 1).build()).withExtras(said);
        JsonNode root = rootOf(new GlbWriter().write(scene));
        assertEquals("bands", root.path("extras").path("markings").path(0).path("shows").asString());
        assertEquals(1.2, root.path("extras").path("markings").path(0).path("radius").asDouble());
    }

    @Test
    void aSceneThatSaysNothingWritesNoExtras() {
        JsonNode root = rootOf(new GlbWriter().write(new MeshBuilder().box(BODY, Placement.IDENTITY, 1, 1, 1).build()));
        assertFalse(root.has("extras"));
    }

    @Test
    void extrasAreAnObject() {
        MeshScene scene = MeshScene.of(new MeshBuilder().box(BODY, Placement.IDENTITY, 1, 1, 1).build());
        assertThrows(IllegalArgumentException.class, () -> scene.withExtras(JSON.createArrayNode()));
    }
}
