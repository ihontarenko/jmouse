package org.jmouse.json.bind;

import org.jmouse.core.access.TypedValue;
import org.jmouse.json.JsonMappingException;
import org.jmouse.mapper.Mapper;
import org.jmouse.mapper.Mappers;
import org.jmouse.mapper.errors.MappingException;
import org.jmouse.mapper.strategy.MappingStrategyContributor;
import org.jmouse.mapper.strategy.MappingStrategyRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * 🧾 A value as JSON gives it — maps, lists, scalars — read into a type by the jMouse mapper: a bean by its setters, a record by
 * its components, a collection, a map, an array, an {@code Optional}, a scalar converted; a type with a {@link JsonDeserializer}
 * by it, wherever the mapping meets that type. A value read into {@code Object} is the value itself.
 *
 * <p>Its mapper is built once, with the deserializers' strategies before the standard ones, and kept: a mapper resolves and
 * keeps its plans per pair of types.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class JsonBinding implements JsonDeserialization {

    private final Mapper mapper;

    public JsonBinding(JsonDeserializers deserializers) {
        List<MappingStrategyContributor> contributors = new ArrayList<>();
        deserializers.registered().forEach((type, deserializer) -> contributors.add(new JsonDeserializerContributor(type, deserializer, this)));
        contributors.addAll(Mappers.DEFAULT_CONTRIBUTORS);
        this.mapper = Mappers.builder().strategyRegistry(new MappingStrategyRegistry(contributors)).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(Object json, TypedValue<T> type) {
        if (json == null || type.getType().getClassType() == Object.class) {
            return (T) json;
        }

        try {
            return mapper.map(json, type);
        } catch (MappingException exception) {
            throw new JsonMappingException("JSON could not be read as " + type.getType() + ": " + exception.getMessage() + becauseOf(exception), exception);
        }
    }

    /** The deepest cause, where it is another: what went wrong at the bottom, which a mapping's own words only wrap. */
    private static String becauseOf(Throwable exception) {
        Throwable deepest = exception;
        while (deepest.getCause() != null && deepest.getCause() != deepest) {
            deepest = deepest.getCause();
        }
        return deepest == exception ? "" : " — because " + deepest.getClass().getSimpleName() + ": " + deepest.getMessage();
    }
}
