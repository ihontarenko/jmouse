package svit.support.context;

import java.util.Map;

/**
 * Interface defining a generic context for managing properties and beans.
 * Provides methods to manipulate key-value properties and integrate with {@link BeanProvider}.
 *
 * <p>Example usage:
 * <pre>{@code
 * Context context = new DefaultContext();
 * context.setProperty("key", "value");
 * String value = context.getProperty("key");
 * System.out.println(value); // Outputs: value
 * }</pre>
 */
@SuppressWarnings({"unused"})
public interface Context extends BeanProvider, BeanProviderAware {

    /**
     * Retrieves all properties as a map.
     *
     * @return a map of properties.
     */
    Map<Object, Object> getProperties();

    /**
     * Copies all properties and the bean provider from another context.
     *
     * @param context the source context to copy from.
     */
    default void copyFrom(Context context) {
        context.getProperties().forEach(this::setProperty);
        setBeanProvider(context.getBeanProvider());
    }

    /**
     * Copies all properties and the bean provider to another context.
     *
     * @param context the target context to copy to.
     */
    default void copyTo(Context context) {
        getProperties().forEach(context::setProperty);
        context.setBeanProvider(getBeanProvider());
    }

    /**
     * Sets a single property by key-value pair.
     *
     * @param key   the key of the property.
     * @param value the value of the property.
     */
    void setProperty(Object key, Object value);

    /**
     * Sets multiple properties from a map.
     *
     * @param properties the map of properties to set.
     */
    default void setProperties(Map<Object, Object> properties) {
        properties.forEach(this::setProperty);
    }

    /**
     * Sets a property with a given value.
     *
     * @param value the property value to set.
     */
    void setProperty(Object value);

    /**
     * Retrieves a property by its key.
     *
     * @param key the key of the property.
     * @param <R> the type of the property value.
     * @return the property value, or {@code null} if not found.
     */
    <R> R getProperty(Object key);

    /**
     * Retrieves a property by its key with a default value.
     *
     * @param key          the key of the property.
     * @param defaultValue the default value to return if the property is not found.
     * @param <R>          the type of the property value.
     * @return the property value, or {@code defaultValue} if not found.
     */
    <R> R getProperty(Object key, Object defaultValue);

    /**
     * Retrieves a required property, throwing an exception if it does not exist.
     *
     * @param key the key of the required property.
     * @return the property value.
     * @throws DefaultContextException if the property is not found.
     */
    default Object requireProperty(Object key) {
        Object property = getProperty(key);

        if (property == null) {
            throw new DefaultContextException(
                    "Context does not contain the required property '%s'.".formatted(key));
        }

        return property;
    }

    /**
     * Checks if a property exists by its key.
     *
     * @param key the key of the property.
     * @return {@code true} if the property exists, {@code false} otherwise.
     */
    boolean hasProperty(Object key);

    /**
     * Checks if the context is in a stopped state.
     *
     * @return {@code true} if processing is stopped, {@code false} otherwise.
     */
    boolean isStopped();

    /**
     * Stops further processing in the context.
     */
    void stopProcessing();
}
