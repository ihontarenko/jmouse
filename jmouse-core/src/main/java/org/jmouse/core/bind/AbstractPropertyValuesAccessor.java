package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.bean.ObjectDescriptor;

/**
 * An abstract base implementation of {@link PropertyValuesAccessor}.
 * <p>
 * This class provides a foundation for concrete data source implementations by storing
 * the underlying source object.
 * </p>
 *
 * @see PropertyValuesAccessor
 */
public abstract class AbstractPropertyValuesAccessor implements PropertyValuesAccessor {

    /**
     * The underlying data source object.
     */
    protected final Object                   source;

    /**
     * The descriptor of underlying data type.
     */
    protected final ObjectDescriptor<Object> descriptor;

    /**
     * Constructs an {@link AbstractPropertyValuesAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    public AbstractPropertyValuesAccessor(Object source) {
        this.source = source;
        this.descriptor = PropertyValuesAccessor.descriptor(source);
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
