package svit.env;

import svit.util.Ordered;

import java.util.Objects;

/**
 * Abstract base class for {@link PropertySource} implementations.
 * <p>
 * This class provides common functionality for property sources, including storage for the source and name,
 * as well as ordering support through the {@link Ordered} interface.
 * </p>
 *
 * @param <T> the type of the underlying property source
 */
abstract public class AbstractPropertySource<T> implements PropertySource<T>, Ordered {

    protected T      source;
    protected String name;
    protected int    order;

    /**
     * Constructs a new {@link AbstractPropertySource} with the specified name and source.
     *
     * @param name   the name of the property source
     * @param source the underlying source of properties
     */
    public AbstractPropertySource(String name, T source) {
        this.name = name;
        this.source = source;
    }

    /**
     * Returns the name of this property source.
     *
     * @return the name of the property source
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the underlying source of this property source.
     *
     * @return the underlying source of properties
     */
    @Override
    public T getSource() {
        return source;
    }

    /**
     * Returns the order or priority of this object.
     * <p>
     * Lower values typically represent higher priority.
     * </p>
     *
     * @return the order value
     */
    @Override
    public int getOrder() {
        return order;
    }

    /**
     * Sets the order or priority of this object.
     *
     * @param order the order value to set
     */
    @Override
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Checks if this property source is equal to another object based on the name.
     *
     * @param object the object to compare
     * @return {@code true} if the object is an {@link AbstractPropertySource} with the same name, {@code false} otherwise
     */
    @Override
    public boolean equals(Object object) {
        if ((object instanceof AbstractPropertySource<?> that)) {
            return Objects.equals(getName(), that.getName());
        }
        return false;
    }

    /**
     * Returns the hash code of this property source based on its name.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }
}
