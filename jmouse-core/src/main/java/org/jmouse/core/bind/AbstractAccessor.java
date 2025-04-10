package org.jmouse.core.bind;

import org.jmouse.core.bind.accessor.NullObjectAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger                LOGGER = LoggerFactory.getLogger(AbstractAccessor.class);
    /**
     * The underlying data source object.
     */
    protected final      Object                source;
    private              ObjectAccessorWrapper wrapper;

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
        } else {
            LOGGER.error("Accessor is not configured properly.");
        }

        return wrapped;
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

    /**
     * Sets the ObjectAccessorWrapper.
     *
     * @param wrapper the factory to set
     */
    @Override
    public void setWrapper(ObjectAccessorWrapper wrapper) {
        this.wrapper = wrapper;
    }


}
