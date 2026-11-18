package org.jmouse.json.reading;

import org.jmouse.json.JsonMappingException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 🩹 A JSON Patch (RFC 6902) applied to a tree as this library reads JSON — maps, lists and scalars —, in place: a document of
 * thousands of items a patch changes one of is that item changed, never copied whole. With it, what the patch changed
 * ({@link ChangedPaths}), for reading again only that ({@link StructuralReading}).
 *
 * <p>A patch that does not fit the tree — a place it names is not there, a test fails — is refused ({@link JsonMappingException}),
 * and the tree may be left part changed: whoever holds it lets it go and is sent it whole.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonTreePatch {

    /** The member of an operation naming what it does, as RFC 6902 writes it: {@code "op"}. */
    public static final String OPERATION = "op";

    /** The member of an operation naming where it acts, a JSON Pointer. */
    public static final String PATH = "path";

    /** The member of a move or a copy naming where its value comes from. */
    public static final String FROM = "from";

    /** The member of an add, a replace or a test holding its value. */
    public static final String VALUE = "value";

    /** The last step of a pointer that adds past an array's end. */
    private static final String PAST_THE_END = "-";

    private JsonTreePatch() {
    }

    /** The tree a patch leaves — the very tree, changed, where it holds the place; a new value where the patch replaced it whole. */
    public static Object applied(Object tree, List<?> operations) {
        Object now = tree;
        for (Object written : operations) {
            now = appliedOne(now, operationOf(written));
        }
        return now;
    }

    /** What the operations of a patch change, read off them. */
    public static ChangedPaths changedBy(List<?> operations) {
        List<JsonPatchChange> changes = new ArrayList<>();
        for (Object written : operations) {
            Map<?, ?> operation = operationOf(written);
            Object from = operation.get(FROM);
            changes.add(new JsonPatchChange(kindOf(operation), String.valueOf(operation.get(PATH)),
                    from == null ? Optional.empty() : Optional.of(String.valueOf(from))));
        }
        return ChangedPaths.of(changes);
    }

    private static Map<?, ?> operationOf(Object written) {
        if (written instanceof Map<?, ?> operation) {
            return operation;
        }
        throw new JsonMappingException("An operation of a patch is an object: " + written);
    }

    private static JsonPatchChange.Operation kindOf(Map<?, ?> operation) {
        return JsonPatchChange.Operation.of(String.valueOf(operation.get(OPERATION)));
    }

    private static Object appliedOne(Object tree, Map<?, ?> operation) {
        List<String> path = ChangedPaths.pointer(String.valueOf(operation.get(PATH)));
        return switch (kindOf(operation)) {
            case ADD -> added(tree, path, operation.get(VALUE));
            case REMOVE -> {
                removed(tree, path);
                yield tree;
            }
            case REPLACE -> replaced(tree, path, operation.get(VALUE));
            case MOVE -> {
                List<String> from = ChangedPaths.pointer(String.valueOf(operation.get(FROM)));
                Object value = at(tree, from);
                removed(tree, from);
                yield added(tree, path, value);
            }
            case COPY -> added(tree, path, at(tree, ChangedPaths.pointer(String.valueOf(operation.get(FROM)))));
            case TEST -> {
                if (!Objects.equals(at(tree, path), operation.get(VALUE))) {
                    throw new JsonMappingException("The test of a patch failed at " + operation.get(PATH));
                }
                yield tree;
            }
        };
    }

    private static Object added(Object tree, List<String> path, Object value) {
        if (path.isEmpty()) {
            return value;
        }
        Object holder = at(tree, path.subList(0, path.size() - 1));
        String last = path.getLast();
        switch (holder) {
            case Map<?, ?> map -> mapOf(map).put(last, value);
            case List<?> list -> {
                List<Object> items = listOf(list);
                items.add(PAST_THE_END.equals(last) ? items.size() : index(last, items.size() + 1), value);
            }
            default -> throw new JsonMappingException("Nothing can be added to a scalar at " + String.join("/", path));
        }
        return tree;
    }

    private static Object replaced(Object tree, List<String> path, Object value) {
        if (path.isEmpty()) {
            return value;
        }
        Object holder = at(tree, path.subList(0, path.size() - 1));
        String last = path.getLast();
        switch (holder) {
            case Map<?, ?> map -> {
                if (!map.containsKey(last)) {
                    throw new JsonMappingException("No member " + last + " to replace");
                }
                mapOf(map).put(last, value);
            }
            case List<?> list -> listOf(list).set(index(last, list.size()), value);
            default -> throw new JsonMappingException("Nothing to replace in a scalar at " + String.join("/", path));
        }
        return tree;
    }

    private static void removed(Object tree, List<String> path) {
        if (path.isEmpty()) {
            throw new JsonMappingException("A document cannot remove itself");
        }
        Object holder = at(tree, path.subList(0, path.size() - 1));
        String last = path.getLast();
        switch (holder) {
            case Map<?, ?> map -> {
                if (!map.containsKey(last)) {
                    throw new JsonMappingException("No member " + last + " to remove");
                }
                mapOf(map).remove(last);
            }
            case List<?> list -> listOf(list).remove(index(last, list.size()));
            default -> throw new JsonMappingException("Nothing to remove from a scalar at " + String.join("/", path));
        }
    }

    /** The value at a place of the tree. */
    private static Object at(Object tree, List<String> path) {
        Object here = tree;
        for (String step : path) {
            here = switch (here) {
                case Map<?, ?> map when map.containsKey(step) -> map.get(step);
                case List<?> list -> list.get(index(step, list.size()));
                default -> throw new JsonMappingException("No place " + step + " in the document");
            };
        }
        return here;
    }

    private static int index(String step, int bound) {
        int index;
        try {
            index = Integer.parseInt(step);
        } catch (NumberFormatException notAnIndex) {
            throw new JsonMappingException("Not an index of an array: " + step);
        }
        if (index < 0 || index >= bound) {
            throw new JsonMappingException("No element " + step + " of " + bound);
        }
        return index;
    }

    /** An object of the tree, to change in place: the reader makes every one a {@link LinkedHashMap}. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(Map<?, ?> map) {
        if (map instanceof LinkedHashMap<?, ?>) {
            return (Map<String, Object>) map;
        }
        throw new JsonMappingException("An object of the document cannot be changed in place");
    }

    /** An array of the tree, to change in place: the reader makes every one an {@link ArrayList}. */
    @SuppressWarnings("unchecked")
    private static List<Object> listOf(List<?> list) {
        if (list instanceof ArrayList<?>) {
            return (List<Object>) list;
        }
        throw new JsonMappingException("An array of the document cannot be changed in place");
    }
}
