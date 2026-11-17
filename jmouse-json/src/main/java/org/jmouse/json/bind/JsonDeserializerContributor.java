package org.jmouse.json.bind;

import org.jmouse.core.Priority;
import org.jmouse.core.access.TypedValue;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.mapper.MappingContext;
import org.jmouse.mapper.strategy.MappingStrategy;
import org.jmouse.mapper.strategy.MappingStrategyContributor;

/**
 * 🧾 A {@link JsonDeserializer} as the mapper takes one: a strategy for exactly its type, asked before every standard one, so the
 * type is read by it wherever the mapping meets it — at the top, as a property, as a component, as an element.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@Priority(Integer.MIN_VALUE + 10)
final class JsonDeserializerContributor implements MappingStrategyContributor {

    private final Class<?>            type;
    private final JsonDeserializer<?> deserializer;
    private final JsonDeserialization deserialization;

    JsonDeserializerContributor(Class<?> type, JsonDeserializer<?> deserializer, JsonDeserialization deserialization) {
        this.type = type;
        this.deserializer = deserializer;
        this.deserialization = deserialization;
    }

    @Override
    public boolean supports(Object source, InferredType targetType, MappingContext context) {
        return targetType != null && targetType.getClassType() == type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MappingStrategy<T> build(Object source, TypedValue<T> typedValue, MappingContext context) {
        return (json, ignored, mapping) -> json == null ? null : (T) deserializer.deserialize(json, deserialization);
    }
}
