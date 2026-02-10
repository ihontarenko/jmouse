package org.jmouse.core.access;

import org.jmouse.core.access.accessor.NullObjectAccessor;
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
public abstract class AbstractAccessor implements ObjectAccessor, AccessorWrapper.Aware {

    private static final Logger          LOGGER = LoggerFactory.getLogger(AbstractAccessor.class);
    /**
     * The underlying data source object.
     */
    protected final      Object          source;
    private              AccessorWrapper wrapper;

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
     * Returns the AccessorWrapper.
     *
     * @return the AccessorWrapper
     */
    @Override
    public AccessorWrapper getWrapper() {
        return wrapper;
    }

    /**
     * Sets the AccessorWrapper.
     *
     * @param wrapper the factory to set
     */
    @Override
    public void setWrapper(AccessorWrapper wrapper) {
        this.wrapper = wrapper;
    }


}
