package org.jmouse.json.patch;

import org.jmouse.core.access.TypedValue;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 🩹 A tree as this library reads JSON — maps, lists and scalars ({@link JsonMapper#read(String)}) —, walked by
 * {@link StructuralReading}, a node read whole through a mapper.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonTreeAccess implements TreeAccess<Object> {

    private final JsonMapper mapper;

    public JsonTreeAccess(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Object> member(Object node, String name) {
        return node instanceof Map<?, ?> object && object.containsKey(name) ? Optional.ofNullable(object.get(name)) : Optional.empty();
    }

    @Override
    public Optional<Object> element(Object node, int index) {
        return node instanceof List<?> array && index >= 0 && index < array.size() ? Optional.ofNullable(array.get(index)) : Optional.empty();
    }

    @Override
    public int size(Object node) {
        return node instanceof List<?> array ? array.size() : -1;
    }

    @Override
    public Iterable<String> names(Object node) {
        return node instanceof Map<?, ?> object ? object.keySet().stream().map(String::valueOf).toList() : List.of();
    }

    @Override
    public Object read(Object node, Type type) {
        return mapper.convert(node, TypedValue.of(InferredType.forType(type)));
    }
}
