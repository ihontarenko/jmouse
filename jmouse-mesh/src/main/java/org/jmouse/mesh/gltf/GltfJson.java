package org.jmouse.mesh.gltf;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.jmouse.mesh.MeshReadException;


/** A glTF document's JSON as a tree; JSON that does not parse, or is not an object, is refused saying where it stops. */
final class GltfJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GltfJson() {
    }

    static JsonNode parse(byte[] json, String name) {
        JsonNode document;
        try {
            document = MAPPER.readTree(json);
        } catch (JacksonException refused) {
            throw new MeshReadException(name + ": its glTF JSON does not parse: " + refused.getOriginalMessage(), refused);
        }
        if (document == null || !document.isObject()) {
            throw new MeshReadException(name + ": its glTF JSON is not an object");
        }
        if (!document.path("asset").path("version").asString("").startsWith("2.")) {
            throw new MeshReadException(name + ": glTF of version '" + document.path("asset").path("version").asString("")
                    + "'; only 2.x is read");
        }
        return document;
    }
}
