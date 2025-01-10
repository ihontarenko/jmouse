package svit.context;

import svit.reflection.Reflections;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;

/**
 * An abstract implementation of the {@link Context} interface.
 * Provides a base for managing key-value properties and integrating with a {@link BeanProvider}.
 *
 * <p>Example usage:
 * <pre>{@code
 * AbstractContext context = new CustomContext();
 * context.setProperty("key", "value");
 * String value = context.getProperty("key");
 * System.out.println(value); // Outputs: value
 * }</pre>
 */
public abstract class AbstractContext implements Context {

    private final Map<Object, Object> properties = new HashMap<>();
    private BeanProvider beanProvider;
    private boolean stopped = false;

    /**
     * Default constructor. Initializes the context without a {@link BeanProvider}.
     */
    public AbstractContext() {
        this(null);
    }

    /**
     * Constructor with a specified {@link BeanProvider}.
     *
     * @param beanProvider the bean provider to associate with this context.
     */
    public AbstractContext(BeanProvider beanProvider) {
        this.beanProvider = beanProvider;
    }

    /**
     * Sets the {@link BeanProvider} for this context.
     *
     * @param beanProvider the bean provider to set.
     */
    @Override
    public void setBeanProvider(BeanProvider beanProvider) {
        this.beanProvider = beanProvider;
    }

    /**
     * Retrieves the current {@link BeanProvider}.
     *
     * @return the associated bean provider.
     */
    @Override
    public BeanProvider getBeanProvider() {
        return beanProvider;
    }

    /**
     * Retrieves a bean by its class type.
     *
     * @param beanClass the class of the bean to retrieve.
     * @param <T>       the type of the bean.
     * @return the bean instance.
     * @throws MissingBeanProviderException if the {@link BeanProvider} is not set.
     */
    @Override
    public <T> T getBean(Class<T> beanClass) {
        return ofNullable(beanProvider).orElseThrow(() -> new MissingBeanProviderException(
                "The BeanProvider has not been provided in this context. Ensure it is set before usage."
        )).getBean(beanClass);
    }

    /**
     * Retrieves a bean by its name and class type.
     *
     * @param beanName  the name of the bean to retrieve.
     * @param beanClass the class of the bean to retrieve.
     * @param <T>       the type of the bean.
     * @return the bean instance.
     * @throws MissingBeanProviderException if the {@link BeanProvider} is not set.
     */
    @Override
    public <T> T getBean(String beanName, Class<T> beanClass) {
        return ofNullable(beanProvider).orElseThrow(() -> new MissingBeanProviderException(
                "The BeanProvider has not been provided in this context. Ensure it is set before usage."
        )).getBean(beanName, beanClass);
    }

    /**
     * Retrieves all properties as a map.
     *
     * @return a map of all properties.
     */
    @Override
    public Map<Object, Object> getProperties() {
        return properties;
    }

    /**
     * Sets a single property by key-value pair.
     *
     * @param key   the key of the property.
     * @param value the value of the property.
     */
    @Override
    public void setProperty(Object key, Object value) {
        properties.put(key, value);
    }

    /**
     * Sets a property using the value's class as the key.
     *
     * @param value the value of the property.
     */
    @Override
    public void setProperty(Object value) {
        Class<?> classKey = Reflections.getUserClass(requireNonNullElse(value, new Object()).getClass());
        setProperty(classKey, value);
    }

    /**
     * Retrieves a property by its key.
     *
     * @param key the key of the property.
     * @param <R> the type of the property value.
     * @return the property value, or {@code null} if not found.
     */
    @Override
    public <R> R getProperty(Object key) {
        return getProperty(key, null);
    }

    /**
     * Retrieves a property by its key with a default value.
     *
     * @param key          the key of the property.
     * @param defaultValue the default value to return if the property is not found.
     * @param <R>          the type of the property value.
     * @return the property value, or {@code defaultValue} if not found.
     */
    @Override
    @SuppressWarnings({"unchecked"})
    public <R> R getProperty(Object key, Object defaultValue) {
        return (R) properties.getOrDefault(key, defaultValue);
    }

    /**
     * Checks if a property exists by its key.
     *
     * @param key the key of the property.
     * @return {@code true} if the property exists, {@code false} otherwise.
     */
    @Override
    public boolean hasProperty(Object key) {
        return properties.containsKey(key);
    }

    /**
     * Checks if the context is in a stopped state.
     *
     * @return {@code true} if processing is stopped, {@code false} otherwise.
     */
    @Override
    public boolean isStopped() {
        return stopped;
    }

    /**
     * Stops further processing in the context.
     */
    @Override
    public void stopProcessing() {
        stopped = true;
    }
}
