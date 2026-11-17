package org.jmouse.json.bind;

import org.jmouse.core.access.TypedValue;

/**
 * 🧾 What a {@link JsonDeserializer} reads its parts with: a value as JSON gives it, read into a type — by that type's
 * deserializer, or bound by the core's {@code Binder}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface JsonDeserialization {

    <T> T read(Object json, TypedValue<T> type);

    default <T> T read(Object json, Class<T> type) {
        return read(json, TypedValue.of(type));
    }
}
