package org.jmouse.json.patch;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 🔍 The JSON Patch between two documents: the operations that change the first into the second, down to the smallest value
 * that changed — arrays element by element, those past the shorter's end added or removed at its end; objects member by
 * member. What a side that last sent a document sends instead of the next one.
 *
 * <pre>{@code
 * JsonPatch change = JsonDifference.between(sentLastTime, answerNow);
 * }</pre>
 */
public final class JsonDifference {

    private JsonDifference() {
    }

    /** The patch that changes {@code before} into {@code after}: empty where they are equal. */
    public static JsonPatch between(JsonNode before, JsonNode after) {
        List<JsonPatchOperation> operations = new ArrayList<>();
        walk(before, after, JsonPointer.empty(), operations);
        return new JsonPatch(operations);
    }

    private static void walk(JsonNode before, JsonNode after, JsonPointer at, List<JsonPatchOperation> operations) {
        if (before == after || before.equals(after)) {
            return;
        }
        if (before.isArray() && after.isArray()) {
            int common = Math.min(before.size(), after.size());
            for (int index = 0; index < common; index++) {
                walk(before.get(index), after.get(index), at.appendIndex(index), operations);
            }
            for (int index = common; index < after.size(); index++) {
                operations.add(new JsonPatchOperation.Add(at.appendProperty("-"), after.get(index)));
            }
            // From the end back, so each index still names the element it meant.
            for (int index = before.size() - 1; index >= common; index--) {
                operations.add(new JsonPatchOperation.Remove(at.appendIndex(index)));
            }
            return;
        }
        if (before.isObject() && after.isObject()) {
            for (Map.Entry<String, JsonNode> member : before.properties()) {
                if (!after.has(member.getKey())) {
                    operations.add(new JsonPatchOperation.Remove(at.appendProperty(member.getKey())));
                }
            }
            for (Map.Entry<String, JsonNode> member : after.properties()) {
                JsonNode was = before.get(member.getKey());
                if (was == null) {
                    operations.add(new JsonPatchOperation.Add(at.appendProperty(member.getKey()), member.getValue()));
                } else {
                    walk(was, member.getValue(), at.appendProperty(member.getKey()), operations);
                }
            }
            return;
        }
        operations.add(new JsonPatchOperation.Replace(at, after));
    }
}
