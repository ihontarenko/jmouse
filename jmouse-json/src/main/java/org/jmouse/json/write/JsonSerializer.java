package org.jmouse.json.write;

/**
 * 🧾 How a value of a type is written: to the {@link JsonOutput} given — a scalar, an object of members, an array of elements —
 * any part of it written by {@link JsonOutput#value(Object)}, so its own parts are written by their serializers in turn.
 *
 * @param <T> the type it writes
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@FunctionalInterface
public interface JsonSerializer<T> {

    void serialize(T value, JsonOutput output);
}
