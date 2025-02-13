package org.jmouse.core.bind;

/**
 * An abstract base implementation of {@link PropertyValueAccessor}.
 * <p>
 * This class provides a foundation for concrete data source implementations by storing
 * the underlying source object.
 * </p>
 *
 * @see PropertyValueAccessor
 */
public abstract class AbstractPropertyValueAccessor implements PropertyValueAccessor {

    /**
     * The underlying data source object.
     */
    protected final Object source;

    /**
     * Constructs an {@link AbstractPropertyValueAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    public AbstractPropertyValueAccessor(Object source) {
        this.source = source;
    }

    /**
     * Retrieves the underlying source object.
     *
     * @return the original source object
     */
    @Override
    public Object unwrap() {
        return source;
    }

}
