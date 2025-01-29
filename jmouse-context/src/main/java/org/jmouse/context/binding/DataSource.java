package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;

import java.util.Map;

import static org.jmouse.core.reflection.Reflections.getShortName;

public interface DataSource {

    DataSource get(String name);

    DataSource get(int index);

    default <T> boolean is(Class<T> type) {
        return type.isAssignableFrom(getSource().getClass());
    }

    default boolean isMap() {
        return is(Map.class);
    }

    default boolean isArray() {
        return is(Object[].class);
    }

    default <T> T as(Class<T> type) {
        if (is(type)) {
            return type.cast(getSource());
        }

        throw new IllegalArgumentException(
                "Data source type '%s' is incompatible with '%s'"
                        .formatted(getShortName(getSource()), getShortName(type)));
    }

    default Object[] asArray() {
        return as(Object[].class);
    }

    @SuppressWarnings({"unchecked"})
    default <K, V> Map<K, V> asMap(Class<K> keyType, Class<V> valueType) {
        Map<K, V> map  = as(Map.class);
        JavaType  type = JavaType.forInstance(map).toMap();

        if (!keyType.isAssignableFrom(type.getFirst().getRawType())) {
            throw new IllegalArgumentException(
                    "Map key type '%s' is not assignable from '%s'"
                        .formatted(keyType, type.getFirst()));
        }

        if (!valueType.isAssignableFrom(type.getFirst().getRawType())) {
            throw new IllegalArgumentException(
                    "Map value type '%s' is not assignable from '%s'"
                            .formatted(keyType, type.getLast()));
        }

        return map;
    }

    default Map<Object, Object> asMap() {
        return as(Map.class);
    }

    Object getSource();

}
