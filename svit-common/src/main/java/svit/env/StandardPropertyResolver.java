package svit.env;

import svit.convert.Converter;

import java.util.HashMap;
import java.util.Map;

/**
 * A standard implementation of {@link PropertyResolver} that supports property retrieval and conversion.
 * <p>
 * This class extends {@link AbstractPropertyResolver} to provide a concrete implementation
 * of property resolution with type conversion capabilities.
 * </p>
 */
public class StandardPropertyResolver extends AbstractPropertyResolver {

    private final Map<Class<?>, Converter<String, Object>> converters = new HashMap<>();

    /**
     * Constructs a {@link StandardPropertyResolver} with the specified {@link PropertySourceRegistry}.
     *
     * @param registry the property source registry to use for resolving properties
     */
    public StandardPropertyResolver(PropertySourceRegistry registry) {
        super(registry);

        converters.put(Integer.class, Integer::parseInt);
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

        if (converters.containsKey(targetType)) {
            value = converters.get(targetType).convert((String) value);
            System.out.println("converted value: " + value);
        }

        return (T) value;
    }
}
