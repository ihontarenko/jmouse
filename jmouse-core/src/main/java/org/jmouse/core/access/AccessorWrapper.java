package org.jmouse.core.access;

/**
 * A factory interface for creating ObjectAccessor instances.
 */
public interface AccessorWrapper {

    /**
     * Wraps the given source object into an ObjectAccessor.
     *
     * @param source the source object to wrap
     * @return an ObjectAccessor instance
     */
    ObjectAccessor wrap(Object source);

    /**
     * Register an {@link ObjectAccessorProvider}.
     *
     * @param provider the valueProvider to register
     */
    void registerProvider(ObjectAccessorProvider provider);

    /**
     * Wraps the given source object into an ObjectAccessor if necessary.
     *
     * @param value the source object to wrap
     * @return an ObjectAccessor instance
     */
    default ObjectAccessor wrapIfNecessary(Object value) {
        return (value instanceof ObjectAccessor accessor) ? accessor : wrap(value);
    }

    /**
     * An interface for ObjectAccessors that require awareness of the factory.
     * Implementing this allows the factory to inject itself into the ObjectAccessor.
     */
    interface Aware {
        /**
         * Sets the AccessorWrapper.
         *
         * @param wrapper the factory to set
         */
        void setWrapper(AccessorWrapper wrapper);

        /**
         * Returns the AccessorWrapper.
         *
         * @return the AccessorWrapper
         */
        AccessorWrapper getWrapper();
    }

}
