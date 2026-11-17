package org.jmouse.json.patch;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/** 🔧 One operation of a JSON Patch (RFC 6902, section 4), its places as JSON Pointers. */
public sealed interface JsonPatchOperation {

    /** Where the operation acts. */
    JsonPointer path();

    /** The operation as it is written: {@code {"op": "replace", "path": "/tracks/3", "value": …}}. */
    default ObjectNode written(JsonNodeFactory nodes) {
        ObjectNode written = nodes.objectNode();
        switch (this) {
            case Add add -> written.put("op", "add").put("path", add.path().toString()).set("value", add.value());
            case Remove remove -> written.put("op", "remove").put("path", remove.path().toString());
            case Replace replace -> written.put("op", "replace").put("path", replace.path().toString()).set("value", replace.value());
            case Move move -> written.put("op", "move").put("from", move.from().toString()).put("path", move.path().toString());
            case Copy copy -> written.put("op", "copy").put("from", copy.from().toString()).put("path", copy.path().toString());
            case Test test -> written.put("op", "test").put("path", test.path().toString()).set("value", test.value());
        }
        return written;
    }

    /** A value put at a place: a member set, an element inserted — {@code -} appends —, the whole document where the place is the root. */
    record Add(JsonPointer path, JsonNode value) implements JsonPatchOperation {
    }

    /** The value at a place taken away. */
    record Remove(JsonPointer path) implements JsonPatchOperation {
    }

    /** The value at a place, which must be there, put in place of. */
    record Replace(JsonPointer path, JsonNode value) implements JsonPatchOperation {
    }

    /** The value at one place taken to another. */
    record Move(JsonPointer from, JsonPointer path) implements JsonPatchOperation {
    }

    /** The value at one place added again at another. */
    record Copy(JsonPointer from, JsonPointer path) implements JsonPatchOperation {
    }

    /** That the value at a place is this one — else the whole patch is refused. */
    record Test(JsonPointer path, JsonNode value) implements JsonPatchOperation {
    }

    /** An operation as it is written: {@code {"op": "replace", "path": "/tracks/3", "value": …}}. */
    static JsonPatchOperation of(JsonNode written) {
        if (!written.isObject()) {
            throw new JsonPatchRefusal("An operation is an object, not " + written.getNodeType());
        }
        String operation = text(written, "op");
        JsonPointer path = pointer(written, "path");
        return switch (operation) {
            case "add" -> new Add(path, value(written));
            case "remove" -> new Remove(path);
            case "replace" -> new Replace(path, value(written));
            case "move" -> new Move(pointer(written, "from"), path);
            case "copy" -> new Copy(pointer(written, "from"), path);
            case "test" -> new Test(path, value(written));
            default -> throw new JsonPatchRefusal("Not an operation of JSON Patch: " + operation);
        };
    }

    private static String text(JsonNode written, String member) {
        JsonNode value = written.get(member);
        if (value == null || !value.isString()) {
            throw new JsonPatchRefusal("An operation needs \"" + member + "\" as a string: " + written);
        }
        return value.asString();
    }

    private static JsonPointer pointer(JsonNode written, String member) {
        try {
            return JsonPointer.compile(text(written, member));
        } catch (IllegalArgumentException malformed) {
            throw new JsonPatchRefusal("Not a JSON Pointer: " + written.get(member), malformed);
        }
    }

    private static JsonNode value(JsonNode written) {
        JsonNode value = written.get("value");
        if (value == null) {
            throw new JsonPatchRefusal("An operation needs \"value\": " + written);
        }
        return value;
    }
}
