package org.jmouse.json;

import org.jmouse.core.access.TypedValue;
import org.jmouse.json.bind.JsonBinding;
import org.jmouse.json.bind.JsonDeserializer;
import org.jmouse.json.bind.JsonDeserializers;
import org.jmouse.json.node.JsonNode;
import org.jmouse.json.syntax.JsonReader;
import org.jmouse.json.write.JsonOutput;
import org.jmouse.json.write.JsonSerializer;
import org.jmouse.json.write.JsonSerializers;
import org.jmouse.json.write.StandardSerializers;

/**
 * 🧾 JSON, configured: how each type is written — a {@link JsonSerializer} by type, the nearest in its hierarchy answering —,
 * how a type is read — a {@link JsonDeserializer} where one is registered, the jMouse mapper for every other —, whether
 * a member holding null is written, and the layout. Immutable and safe to share; {@link #toBuilder()} makes another from it.
 *
 * <p>A serializer or deserializer registered here replaces the standard one for its type wherever that type stands — at the
 * top, as a member, as an element, as a map's value.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonMapper {

    private static final JsonReader READER = new JsonReader();

    private final JsonSerializers   serializers;
    private final JsonDeserializers deserializers;
    private final JsonBinding       binding;
    private final JsonInclusion     inclusion;
    private final JsonLayout        layout;

    private JsonMapper(Builder builder) {
        this.serializers = builder.serializers.copy();
        this.deserializers = builder.deserializers.copy();
        this.binding = new JsonBinding(deserializers);
        this.inclusion = builder.inclusion;
        this.layout = builder.layout;
    }

    /** A builder with every standard serializer, members with null written, compact. */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder holding what this mapper holds. */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** The value a JSON text writes. */
    public Object read(String text) {
        return READER.read(text);
    }

    /** The tree of a JSON text. */
    public JsonNode parse(String text) {
        return READER.parse(text);
    }

    /** A JSON text read into {@code type}. */
    public <T> T read(String text, Class<T> type) {
        return read(text, TypedValue.of(type));
    }

    /** A JSON text read into a type with its type arguments. */
    public <T> T read(String text, TypedValue<T> type) {
        return convert(READER.read(text), type);
    }

    /** A value as JSON gives it — maps, lists, scalars — read into {@code type}: what {@link #read(String, TypedValue)} does past the text. */
    public <T> T convert(Object json, TypedValue<T> type) {
        return binding.read(json, type);
    }

    /** The JSON text of a value, in this mapper's layout. */
    public String write(Object value) {
        return write(value, layout);
    }

    /** The JSON text of a value, laid out as {@code layout} says. */
    public String write(Object value, JsonLayout layout) {
        return JsonOutput.text(layout, inclusion, serializers, output -> output.value(value));
    }

    public JsonInclusion inclusion() {
        return inclusion;
    }

    public JsonLayout layout() {
        return layout;
    }

    /** What a {@link JsonMapper} is made of, set step by step. */
    public static final class Builder {

        private final JsonSerializers   serializers;
        private final JsonDeserializers deserializers;
        private       JsonInclusion     inclusion = JsonInclusion.ALL;
        private       JsonLayout        layout    = JsonLayout.COMPACT;

        private Builder() {
            this.serializers = StandardSerializers.registered();
            this.deserializers = new JsonDeserializers();
        }

        private Builder(JsonMapper mapper) {
            this.serializers = mapper.serializers.copy();
            this.deserializers = mapper.deserializers.copy();
            this.inclusion = mapper.inclusion;
            this.layout = mapper.layout;
        }

        /** {@code type}, and every type below it that has no serializer nearer, written by {@code serializer}. */
        public <T> Builder serializer(Class<T> type, JsonSerializer<? super T> serializer) {
            serializers.register(type, serializer);
            return this;
        }

        /** {@code type} read by {@code deserializer}, wherever it is read into. */
        public <T> Builder deserializer(Class<T> type, JsonDeserializer<? extends T> deserializer) {
            deserializers.register(type, deserializer);
            return this;
        }

        /** Whether an object's member holding null is written. */
        public Builder inclusion(JsonInclusion inclusion) {
            this.inclusion = inclusion;
            return this;
        }

        /** How a written text is laid out. */
        public Builder layout(JsonLayout layout) {
            this.layout = layout;
            return this;
        }

        public JsonMapper build() {
            return new JsonMapper(this);
        }
    }
}
