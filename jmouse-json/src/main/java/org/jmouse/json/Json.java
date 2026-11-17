package org.jmouse.json;

import org.jmouse.core.access.TypedValue;
import org.jmouse.json.node.JsonNode;

/**
 * 🧾 JSON by the default {@link JsonMapper}: a text read to the value it writes or into a type, a value written back. Read
 * strictly, as RFC 8259 writes JSON. A mapper of its own — serializers and deserializers by type, inclusion, layout — is
 * {@link JsonMapper#builder()}'s.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Json {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Json() {
    }

    /** The mapper behind these methods: every standard serializer and deserializer, members with null written, compact. */
    public static JsonMapper mapper() {
        return MAPPER;
    }

    /** The value a JSON text writes: an object a map in its order, an array a list, a number as jME reads it. */
    public static Object read(String text) {
        return MAPPER.read(text);
    }

    /** A JSON text read into {@code type}. */
    public static <T> T read(String text, Class<T> type) {
        return MAPPER.read(text, type);
    }

    /** A JSON text read into a type with its type arguments: {@code TypedValue.ofList(Point.class)}. */
    public static <T> T read(String text, TypedValue<T> type) {
        return MAPPER.read(text, type);
    }

    /** The tree of a JSON text, each value a node. */
    public static JsonNode parse(String text) {
        return MAPPER.parse(text);
    }

    /** The JSON text of a value. */
    public static String write(Object value) {
        return MAPPER.write(value);
    }

    /** The JSON text of a value, laid out as {@code layout} says. */
    public static String write(Object value, JsonLayout layout) {
        return MAPPER.write(value, layout);
    }
}
