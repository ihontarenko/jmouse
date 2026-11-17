package org.jmouse.json.patch;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ContainerNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 🩹 A JSON Patch (RFC 6902): the operations that change one JSON document into another — add, remove, replace, move, copy,
 * test —, each naming its place by a JSON Pointer (RFC 6901). What a side keeping a document is sent instead of the document
 * again: a part moved on a board of a thousand tracks is a few operations, not the board.
 *
 * <pre>{@code
 * JsonNode kept = mapper.readTree(layout);
 * kept = JsonPatch.of(mapper.readTree(sent)).appliedTo(kept);
 * }</pre>
 *
 * <p>A patch is applied as a whole or not at all ({@link #appliedTo}): an operation that cannot be done — a place that is not
 * there, a test that fails — refuses the patch ({@link JsonPatchRefusal}) and the document is left as it was. {@link #applyInPlace}
 * changes the document itself, for a holder that would rather ask for the whole document again after a refusal than copy it
 * before every patch.</p>
 */
public final class JsonPatch {

    private final List<JsonPatchOperation> operations;

    public JsonPatch(List<JsonPatchOperation> operations) {
        this.operations = List.copyOf(operations);
    }

    /** A patch as it is written: a JSON array of operations, each an object with {@code op} and {@code path}. */
    public static JsonPatch of(JsonNode written) {
        if (!written.isArray()) {
            throw new JsonPatchRefusal("A JSON Patch is an array of operations, not " + written.getNodeType());
        }
        List<JsonPatchOperation> operations = new ArrayList<>();
        for (JsonNode operation : written) {
            operations.add(JsonPatchOperation.of(operation));
        }
        return new JsonPatch(operations);
    }

    public List<JsonPatchOperation> operations() {
        return operations;
    }

    public boolean isEmpty() {
        return operations.isEmpty();
    }

    /** The patch as it is written: an array of its operations. */
    public ArrayNode written(JsonNodeFactory nodes) {
        ArrayNode written = nodes.arrayNode();
        operations.forEach(operation -> written.add(operation.written(nodes)));
        return written;
    }

    /** The document as this patch leaves it — a copy; the document itself is never changed, and is all there is after a refusal. */
    public JsonNode appliedTo(JsonNode document) {
        return applyInPlace(document.deepCopy());
    }

    /**
     * Changes the document itself, and answers it — or, where the patch replaces the whole document, the new one. ⚠️ Not undone
     * after a refusal: the operations before the refused one stay done.
     */
    public JsonNode applyInPlace(JsonNode document) {
        JsonNode root = document;
        for (JsonPatchOperation operation : operations) {
            root = apply(root, operation);
        }
        return root;
    }

    private static JsonNode apply(JsonNode root, JsonPatchOperation operation) {
        return switch (operation) {
            case JsonPatchOperation.Add add -> added(root, add.path(), add.value().deepCopy());
            case JsonPatchOperation.Remove remove -> {
                removed(root, remove.path());
                yield root;
            }
            case JsonPatchOperation.Replace replace -> {
                if (replace.path().matches()) {
                    yield replace.value().deepCopy();
                }
                removed(root, replace.path());
                yield added(root, replace.path(), replace.value().deepCopy());
            }
            case JsonPatchOperation.Move move -> {
                if (move.path().toString().startsWith(move.from().toString() + "/")) {
                    throw new JsonPatchRefusal("Cannot move " + move.from() + " into itself, to " + move.path());
                }
                JsonNode value = valueAt(root, move.from());
                if (move.from().matches()) {
                    yield value;
                }
                removed(root, move.from());
                yield added(root, move.path(), value);
            }
            case JsonPatchOperation.Copy copy -> added(root, copy.path(), valueAt(root, copy.from()).deepCopy());
            case JsonPatchOperation.Test test -> {
                if (!valueAt(root, test.path()).equals(test.value())) {
                    throw new JsonPatchRefusal("The test of " + test.path() + " failed: it is not " + test.value());
                }
                yield root;
            }
        };
    }

    /** The value at a place; refused where there is none. */
    private static JsonNode valueAt(JsonNode root, JsonPointer path) {
        JsonNode value = root.at(path);
        if (value.isMissingNode()) {
            throw new JsonPatchRefusal("Nothing is at " + path);
        }
        return value;
    }

    /** The object or array a place is in: what an operation changes. */
    private static ContainerNode<?> parentOf(JsonNode root, JsonPointer path) {
        JsonNode parent = root.at(path.head());
        if (!(parent instanceof ContainerNode<?> container)) {
            throw new JsonPatchRefusal("Nothing to change at " + path + ": " + path.head() + " is not an object or an array");
        }
        return container;
    }

    private static JsonNode added(JsonNode root, JsonPointer path, JsonNode value) {
        if (path.matches()) {
            return value;
        }
        String last = path.last().getMatchingProperty();
        switch (parentOf(root, path)) {
            case ObjectNode object -> object.set(last, value);
            case ArrayNode array -> {
                if (last.equals("-")) {
                    array.add(value);
                } else {
                    int index = indexOf(path, array.size());
                    array.insert(index, value);
                }
            }
            default -> throw new JsonPatchRefusal("Nothing to add to at " + path);
        }
        return root;
    }

    private static void removed(JsonNode root, JsonPointer path) {
        if (path.matches()) {
            throw new JsonPatchRefusal("The whole document cannot be removed");
        }
        String last = path.last().getMatchingProperty();
        switch (parentOf(root, path)) {
            case ObjectNode object -> {
                if (object.remove(last) == null) {
                    throw new JsonPatchRefusal("Nothing is at " + path);
                }
            }
            case ArrayNode array -> array.remove(indexOf(path, array.size() - 1));
            default -> throw new JsonPatchRefusal("Nothing to remove at " + path);
        }
    }

    /** The array index a place's last step names: digits, no sign, no leading zero (RFC 6901), at most {@code largest}. */
    private static int indexOf(JsonPointer path, int largest) {
        String step = path.last().getMatchingProperty();
        if (!step.matches("0|[1-9][0-9]{0,9}")) {
            throw new JsonPatchRefusal("Not an array index: " + step + " in " + path);
        }
        long index = Long.parseLong(step);
        if (index > largest) {
            throw new JsonPatchRefusal("Past the end of the array: " + path);
        }
        return (int) index;
    }
}
