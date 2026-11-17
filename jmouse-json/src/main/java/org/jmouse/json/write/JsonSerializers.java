package org.jmouse.json.write;

import org.jmouse.json.JsonMappingException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🧾 The serializers of a mapper, by type, and which one writes a value: the one registered for its class; else for the nearest
 * class above it; else for the nearest interface, its own before those of its classes above; else, for an array, the
 * {@link #arrays() array serializer}; else the {@link #fallback() fallback}. The answer is kept for each class.
 *
 * <p>A registration replaces any for the same type: a serializer registered for {@code Number} writes every number that has no
 * nearer one.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonSerializers {

    private final Map<Class<?>, JsonSerializer<?>> registered;
    private final Map<Class<?>, JsonSerializer<?>> resolved = new ConcurrentHashMap<>();
    private       JsonSerializer<Object>           arrays;
    private       JsonSerializer<Object>           fallback;

    public JsonSerializers(JsonSerializer<Object> arrays, JsonSerializer<Object> fallback) {
        this.registered = new LinkedHashMap<>();
        this.arrays = arrays;
        this.fallback = fallback;
    }

    private JsonSerializers(JsonSerializers original) {
        this.registered = new LinkedHashMap<>(original.registered);
        this.arrays = original.arrays;
        this.fallback = original.fallback;
    }

    /** {@code type}, and every type below it with no nearer serializer, written by {@code serializer}. */
    public <T> void register(Class<T> type, JsonSerializer<? super T> serializer) {
        registered.put(type, serializer);
        resolved.clear();
    }

    /** What writes an array with no serializer of its own class. */
    public JsonSerializer<Object> arrays() {
        return arrays;
    }

    /** What writes a value no registered serializer reaches. */
    public JsonSerializer<Object> fallback() {
        return fallback;
    }

    public void arrays(JsonSerializer<Object> arrays) {
        this.arrays = arrays;
        resolved.clear();
    }

    public void fallback(JsonSerializer<Object> fallback) {
        this.fallback = fallback;
        resolved.clear();
    }

    /** A registry holding what this one holds, changed apart from it. */
    public JsonSerializers copy() {
        return new JsonSerializers(this);
    }

    /** {@code value} written by the serializer of its type. */
    @SuppressWarnings("unchecked")
    void serialize(Object value, JsonOutput output) {
        JsonSerializer<Object> serializer = (JsonSerializer<Object>) serializerOf(value.getClass());
        try {
            serializer.serialize(value, output);
        } catch (JsonMappingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new JsonMappingException("A " + value.getClass().getName() + " could not be written as JSON: " + exception.getMessage(), exception);
        }
    }

    /** The serializer that writes a value of {@code type}. */
    public JsonSerializer<?> serializerOf(Class<?> type) {
        return resolved.computeIfAbsent(type, this::resolve);
    }

    private JsonSerializer<?> resolve(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            JsonSerializer<?> serializer = registered.get(current);
            if (serializer != null) {
                return serializer;
            }
        }

        JsonSerializer<?> byInterface = nearestInterface(type);
        if (byInterface != null) {
            return byInterface;
        }

        return type.isArray() ? arrays : fallback;
    }

    /** The serializer of the nearest interface: breadth first, a class's own interfaces before those of the classes above it. */
    private JsonSerializer<?> nearestInterface(Class<?> type) {
        Deque<Class<?>> waiting = new ArrayDeque<>();
        Set<Class<?>>   seen    = new HashSet<>();

        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            waiting.addAll(java.util.List.of(current.getInterfaces()));
        }

        while (!waiting.isEmpty()) {
            Class<?> candidate = waiting.poll();
            if (!seen.add(candidate)) {
                continue;
            }
            JsonSerializer<?> serializer = registered.get(candidate);
            if (serializer != null) {
                return serializer;
            }
            waiting.addAll(java.util.List.of(candidate.getInterfaces()));
        }

        return null;
    }
}
