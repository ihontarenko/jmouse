package org.jmouse.json.reading;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 🩹 A patched JSON document read again by reading only what the patch changed: the value read before is walked down the places the
 * patch reached ({@link ChangedPaths}), and everything off them is the very value it was. A record is made again of its components —
 * those a change reached read again, each in its turn the same way, the others its own —; a list or a map of the same length or keys
 * likewise by its elements; anything else, and any place changed whole, is read whole. A document of thousands of items a step of a
 * hand changed one of costs that one item to read, not the thousands.
 *
 * <p>The tree is any library's ({@link TreeAccess}); what it is read into is plain records, lists and maps.</p>
 *
 * @param <N> what a node of the tree is
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class StructuralReading<N> {

    private final TreeAccess<N> tree;

    public StructuralReading(TreeAccess<N> tree) {
        this.tree = tree;
    }

    /**
     * The document as it now stands — {@code node} —, read as {@code type}: {@code before} — the same document read before the patch —
     * kept wherever the patch changed nothing.
     */
    public Object read(Object before, N node, Type type, ChangedPaths changed) {
        if (before != null && changed.isEmpty()) {
            return before;
        }
        if (before == null || changed.isWhole()) {
            return tree.read(node, type);
        }
        Class<?> raw = rawOf(type);
        if (raw.isRecord()) {
            return record(before, node, raw, changed).orElseGet(() -> tree.read(node, type));
        }
        if (List.class.isAssignableFrom(raw) && before instanceof List<?> list) {
            return list(list, node, argumentOf(type, 0), changed).orElseGet(() -> tree.read(node, type));
        }
        if (Map.class.isAssignableFrom(raw) && before instanceof Map<?, ?> map) {
            return map(map, node, argumentOf(type, 1), changed).orElseGet(() -> tree.read(node, type));
        }
        return tree.read(node, type);
    }

    /** A record made again of its components, those a change reached read again; empty where one is not in the tree any more. */
    private Optional<Object> record(Object before, N node, Class<?> type, ChangedPaths changed) {
        RecordComponent[] components = type.getRecordComponents();
        Object[] values = new Object[components.length];
        Class<?>[] types = new Class<?>[components.length];
        try {
            for (int index = 0; index < components.length; index++) {
                RecordComponent component = components[index];
                types[index] = component.getType();
                Object was = component.getAccessor().invoke(before);
                ChangedPaths below = changed.at(component.getName());
                if (below.isEmpty()) {
                    values[index] = was;
                    continue;
                }
                Optional<N> now = tree.member(node, component.getName());
                if (now.isEmpty()) {
                    return Optional.empty();
                }
                values[index] = read(was, now.get(), component.getGenericType(), below);
            }
            return Optional.of(type.getDeclaredConstructor(types).newInstance(values));
        } catch (ReflectiveOperationException unmade) {
            return Optional.empty();
        }
    }

    /** A list of the same length made again of its elements, those a change reached read again; empty where its length changed. */
    private Optional<Object> list(List<?> before, N node, Type element, ChangedPaths changed) {
        if (tree.size(node) != before.size()) {
            return Optional.empty();
        }
        List<Object> now = new ArrayList<>(before);
        for (int index = 0; index < now.size(); index++) {
            ChangedPaths below = changed.at(String.valueOf(index));
            if (!below.isEmpty()) {
                Optional<N> item = tree.element(node, index);
                if (item.isEmpty()) {
                    return Optional.empty();
                }
                now.set(index, read(before.get(index), item.get(), element, below));
            }
        }
        return Optional.of(Collections.unmodifiableList(now));
    }

    /** A map of the same keys made again of its values, those a change reached read again; empty where its keys changed. */
    private Optional<Object> map(Map<?, ?> before, N node, Type value, ChangedPaths changed) {
        Set<String> names = new HashSet<>();
        tree.names(node).forEach(names::add);
        Set<String> had = new HashSet<>();
        before.keySet().forEach(key -> had.add(String.valueOf(key)));
        if (!names.equals(had)) {
            return Optional.empty();
        }
        Map<Object, Object> now = new LinkedHashMap<>(before);
        for (Map.Entry<?, ?> entry : before.entrySet()) {
            String name = String.valueOf(entry.getKey());
            ChangedPaths below = changed.at(name);
            if (!below.isEmpty()) {
                Optional<N> item = tree.member(node, name);
                if (item.isEmpty()) {
                    return Optional.empty();
                }
                now.put(entry.getKey(), read(entry.getValue(), item.get(), value, below));
            }
        }
        return Optional.of(Collections.unmodifiableMap(now));
    }

    private static Class<?> rawOf(Type type) {
        if (type instanceof Class<?> raw) {
            return raw;
        }
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw) {
            return raw;
        }
        return Object.class;
    }

    /** A type's argument at a place — a list's element, a map's value —; {@code Object} where it says none. */
    private static Type argumentOf(Type type, int place) {
        if (type instanceof ParameterizedType parameterized && parameterized.getActualTypeArguments().length > place) {
            return parameterized.getActualTypeArguments()[place];
        }
        return Object.class;
    }
}
