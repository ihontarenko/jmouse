package org.jmouse.core.mapping.mapper;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * A mapper that converts an object to a {@link Map} of its field names and values.
 * <p>
 * This class uses reflection to retrieve private fields and map their values. It also supports reverse mapping
 * from a {@link Map} to an object.
 * </p>
 * @see Mapper
 * @see Reflections
 */
public class ObjectFieldMapper implements Mapper<Object, Map<String, Object>> {

    /**
     * Converts an object to a {@link Map} of its field names and values.
     *
     * @param source the source object to map
     * @return a {@link Map} containing the field names and their corresponding values
     * <p>
     * This method retrieves all private fields from the source object's class and populates a {@link Map} with their values.
     * </p>
     */
    @Override
    public Map<String, Object> map(Object source) {
        Map<String, Object> data = new HashMap<>();
        map(source, data);
        return data;
    }

    /**
     * Throws {@link UnsupportedOperationException}, as reverse mapping to a new object is not supported.
     *
     * @param source the source {@link Map} to reverse map
     * @return nothing, as this method always throws an exception
     * @throws UnsupportedOperationException when called
     */
    @Override
    public Object reverse(Map<String, Object> source) {
        throw new UnsupportedOperationException();
    }

    /**
     * Maps the fields of the given object to the provided {@link Map}.
     *
     * @param source      the source object to map
     * @param destination the destination {@link Map} to populate
     * <p>
     * This method retrieves private fields of the source object and populates the destination {@link Map}
     * with their names and values.
     * </p>
     * @see Reflections#getClassFields(Class, int)
     * @see Reflections#getFieldValue(Object, Field)
     */
    @Override
    public void map(Object source, Map<String, Object> destination) {
        for (Field field : Reflections.getClassFields(source.getClass(), Modifier.PRIVATE)) {
            destination.put(field.getName(), Reflections.getFieldValue(source, field));
        }
    }

    /**
     * Populates the fields of an object from the given {@link Map}.
     *
     * @param source      the source {@link Map} containing field names and values
     * @param destination the destination object to populate
     * <p>
     * This method sets the fields of the destination object with the corresponding values from the source {@link Map}.
     * </p>
     * @see Reflections#setFieldValue(Object, String, Object)
     */
    @Override
    public void reverse(Map<String, Object> source, Object destination) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Reflections.setFieldValue(destination, entry.getKey(), entry.getValue());
        }
    }

}
