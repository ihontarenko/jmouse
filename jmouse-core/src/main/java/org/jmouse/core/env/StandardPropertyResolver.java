package org.jmouse.core.env;

import org.jmouse.core.convert.ClassPair;
import org.jmouse.core.convert.Conversion;

/**
 * A standard implementation of {@link PropertyResolver} that supports property retrieval and conversion.
 * <p>
 * This class extends {@link AbstractPropertyResolver} to provide a concrete implementation
 * of property resolution with type conversion capabilities.
 * </p>
 */
public class StandardPropertyResolver extends AbstractPropertyResolver {

    private final Conversion conversion;

    public StandardPropertyResolver() {
        conversion = new PropertyValueConversion();
    }

    /**
     * Retrieves the property value for the specified name and converts it to the target type.
     *
     * @param name       the name of the property to retrieve
     * @param targetType the target type to convert the property value to
     * @param <T>        the type of the property value
     * @return the converted property value, or {@code null} if the property is not found
     * @throws ClassCastException if the conversion fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String name, Class<T> targetType) {
        Object value = getRawProperty(name);

        if (value != null) {
            ClassPair pair = ClassPair.of(value.getClass(), targetType);
            if (conversion.hasConverter(pair)) {
                value = conversion.convert(value, targetType);
            }
        }

        return (T) value;
    }
}
