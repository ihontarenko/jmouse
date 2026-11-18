package org.jmouse.json.patch;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/** 🔧 One operation of a JSON Patch (RFC 6902, section 4), its places as JSON Pointers. */
public sealed interface JsonPatchOperation {

    String KEY_FROM    = "from";
    String KEY_PATH    = "path";
    String KEY_VALUE   = "value";
    String KEY_OP      = "op";
    String KEY_REPLACE = "replace";
    String KEY_MOVE    = "move";
    String KEY_COPY    = "copy";
    String KEY_REMOVE  = "remove";
    String KEY_ADD     = "add";
    String KEY_TEST    = "test";

    /** Where the operation acts. */
    JsonPointer path();

    /** The operation as it is written: {@code {"op": "replace", "path": "/tracks/3", "value": …}}. */
    default ObjectNode written(JsonNodeFactory nodes) {
        ObjectNode written = nodes.objectNode();
        switch (this) {
            case Add add -> written.put(KEY_OP, KEY_ADD).put(KEY_PATH, add.path().toString()).set(KEY_VALUE, add.value());
            case Remove remove -> written.put(KEY_OP, KEY_REMOVE).put(KEY_PATH, remove.path().toString());
            case Replace replace -> written.put(KEY_OP, KEY_REPLACE).put(KEY_PATH, replace.path().toString()).set(KEY_VALUE, replace.value());
            case Move move -> written.put(KEY_OP, KEY_MOVE).put(KEY_FROM, move.from().toString()).put(KEY_PATH, move.path().toString());
            case Copy copy -> written.put(KEY_OP, KEY_COPY).put(KEY_FROM, copy.from().toString()).put(KEY_PATH, copy.path().toString());
            case Test test -> written.put(KEY_OP, KEY_TEST).put(KEY_PATH, test.path().toString()).set(KEY_VALUE, test.value());
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
        String operation = text(written, KEY_OP);
        JsonPointer path = pointer(written, KEY_PATH);
        return switch (operation) {
            case KEY_ADD -> new Add(path, value(written));
            case KEY_REMOVE -> new Remove(path);
            case KEY_REPLACE -> new Replace(path, value(written));
            case KEY_MOVE -> new Move(pointer(written, KEY_FROM), path);
            case KEY_COPY -> new Copy(pointer(written, KEY_FROM), path);
            case KEY_TEST -> new Test(path, value(written));
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
        JsonNode value = written.get(KEY_VALUE);
        if (value == null) {
            throw new JsonPatchRefusal("An operation needs \"value\": " + written);
        }
        return value;
    }
}
