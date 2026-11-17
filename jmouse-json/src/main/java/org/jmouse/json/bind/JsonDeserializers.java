package org.jmouse.json.bind;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🧾 The deserializers of a mapper, each for exactly the type it reads into: a type has one or is bound by the core's
 * {@code Binder}.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonDeserializers {

    private final Map<Class<?>, JsonDeserializer<?>> registered;

    public JsonDeserializers() {
        this.registered = new LinkedHashMap<>();
    }

    private JsonDeserializers(JsonDeserializers original) {
        this.registered = new LinkedHashMap<>(original.registered);
    }

    public <T> void register(Class<T> type, JsonDeserializer<? extends T> deserializer) {
        registered.put(type, deserializer);
    }

    /** Every registration, by the type read into. */
    public Map<Class<?>, JsonDeserializer<?>> registered() {
        return Map.copyOf(registered);
    }

    /** A registry holding what this one holds, changed apart from it. */
    public JsonDeserializers copy() {
        return new JsonDeserializers(this);
    }
}
