package org.jmouse.common.mapping.mapper;

import org.jmouse.common.mapping.Mapper;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * A mapper that converts an bean to a {@link Map} of its field names and values.
 * <p>
 * This class uses reflection to retrieve private fields and map their values. It also supports reverse mapping
 * from a {@link Map} to an bean.
 * </p>
 * @see Mapper
 * @see Reflections
 */
public class ObjectFieldMapper implements Mapper<Object, Map<String, Object>> {

    /**
     * Converts an bean to a {@link Map} of its field names and values.
     *
     * @param source the source bean to map
     * @return a {@link Map} containing the field names and their corresponding values
     * <p>
     * This method retrieves all private fields from the source bean's class and populates a {@link Map} with their values.
     * </p>
     */
    @Override
    public Map<String, Object> map(Object source) {
        Map<String, Object> data = new HashMap<>();
        map(source, data);
        return data;
    }

    /**
     * Throws {@link UnsupportedOperationException}, as reverse mapping to a new bean is not supported.
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
     * Maps the fields of the given bean to the provided {@link Map}.
     *
     * @param source      the source bean to map
     * @param destination the destination {@link Map} to populate
     * <p>
     * This method retrieves private fields of the source bean and populates the destination {@link Map}
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
     * Populates the fields of an bean from the given {@link Map}.
     *
     * @param source      the source {@link Map} containing field names and values
     * @param destination the destination bean to populate
     * <p>
     * This method sets the fields of the destination bean with the corresponding values from the source {@link Map}.
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
