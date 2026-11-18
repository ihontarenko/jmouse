package org.jmouse.cad.live;

import org.jmouse.json.patch.JsonPatch;
import org.jmouse.json.patch.JsonPatchOperation;
import org.jmouse.json.reading.ChangedPaths;
import org.jmouse.json.reading.JsonPatchChange;
import org.jmouse.json.reading.StructuralReading;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The layouts a pcb's session holds, read: each slot's tree and the layout read from it, so an ask reads a layout once, not at every
 * step of a hand — and a layout a patch changed is read again only where the patch reached ({@link StructuralReading}), everything
 * else of it the very value read before. A pcb of thousands of items was read whole again at every step, and that was most of
 * what a step cost the server.
 *
 * <p>Read and written on the socket's own thread alone, as the trees are.</p>
 */
final class LayoutReading {

    /** A slot's tree and the layout read from it. */
    private record Kept(JsonNode tree, Object read) {
    }

    private final StructuralReading<JsonNode> reading;
    private final Map<String, Kept>           kept = new HashMap<>();

    LayoutReading(ObjectMapper mapper) {
        this.reading = new StructuralReading<>(new JacksonTreeAccess(mapper));
    }

    /**
     * The layout a slot now holds, read as {@code type}: the one read before where its tree is the very tree; one a patch changed,
     * read again where the patch reached; anything else read whole.
     */
    Object read(String slot, JsonNode tree, Optional<JsonPatch> patch, Class<?> type) {
        Kept before = kept.get(slot);
        Object was = before != null && type.isInstance(before.read()) ? before.read() : null;
        ChangedPaths changed = was == null ? ChangedPaths.none()
                : before.tree() == tree ? ChangedPaths.none()
                : patch.map(LayoutReading::changedBy).orElse(null);
        Object read = changed == null ? reading.read(null, tree, type, ChangedPaths.none()) : reading.read(was, tree, type, changed);
        kept.put(slot, new Kept(tree, read));
        return read;
    }

    /** A slot's layout let go: the next one sent to it is read whole. */
    void forget(String slot) {
        kept.remove(slot);
    }

    void clear() {
        kept.clear();
    }

    /** Where a patch changed a layout. */
    private static ChangedPaths changedBy(JsonPatch patch) {
        return ChangedPaths.of(patch.operations().stream().map(LayoutReading::changeOf).toList());
    }

    private static JsonPatchChange changeOf(JsonPatchOperation operation) {
        return switch (operation) {
            case JsonPatchOperation.Add add -> JsonPatchChange.at(JsonPatchChange.Operation.ADD, add.path().toString());
            case JsonPatchOperation.Remove remove -> JsonPatchChange.at(JsonPatchChange.Operation.REMOVE, remove.path().toString());
            case JsonPatchOperation.Replace replace -> JsonPatchChange.at(JsonPatchChange.Operation.REPLACE, replace.path().toString());
            case JsonPatchOperation.Move move -> new JsonPatchChange(JsonPatchChange.Operation.MOVE, move.path().toString(),
                    Optional.of(move.from().toString()));
            case JsonPatchOperation.Copy copy -> new JsonPatchChange(JsonPatchChange.Operation.COPY, copy.path().toString(),
                    Optional.of(copy.from().toString()));
            case JsonPatchOperation.Test test -> JsonPatchChange.at(JsonPatchChange.Operation.TEST, test.path().toString());
        };
    }
}
