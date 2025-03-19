package org.jmouse.core.bind;

import org.jmouse.core.bind.accessor.NullObjectAccessor;

/**
 * An abstract base implementation of {@link ObjectAccessor}.
 * <p>
 * This class provides a foundation for concrete data source implementations by storing
 * the underlying source object.
 * </p>
 *
 * @see ObjectAccessor
 */
public abstract class AbstractAccessor implements ObjectAccessor, ObjectAccessorWrapper.Aware {

    private ObjectAccessorWrapper wrapper;

    /**
     * The underlying data source object.
     */
    protected final Object source;

    /**
     * Constructs an {@link AbstractAccessor} with the given source object.
     *
     * @param source the source object to wrap
     */
    public AbstractAccessor(Object source) {
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

    /**
     * Creates a {@link ObjectAccessor} instance from the given source object.
     *
     * @param source the source object
     * @return a {@link ObjectAccessor} instance wrapping the source
     */
    @Override
    public ObjectAccessor wrap(Object source) {
        ObjectAccessor wrapped = new NullObjectAccessor();

        if (getWrapper() != null) {
            wrapped = getWrapper().wrap(source);
        }

        return wrapped;
    }

    /**
     * Sets the ObjectAccessorWrapper.
     *
     * @param wrapper the factory to set
     */
    @Override
    public void setWrapper(ObjectAccessorWrapper wrapper) {
        this.wrapper = wrapper;
    }

    /**
     * Returns the ObjectAccessorWrapper.
     *
     * @return the ObjectAccessorWrapper
     */
    @Override
    public ObjectAccessorWrapper getWrapper() {
        return wrapper;
    }



}
