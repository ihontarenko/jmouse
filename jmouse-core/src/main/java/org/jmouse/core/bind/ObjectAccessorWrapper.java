package org.jmouse.core.bind;

/**
 * A factory interface for creating ObjectAccessor instances.
 */
public interface ObjectAccessorWrapper {

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
     * An interface for ObjectAccessors that require awareness of the factory.
     * Implementing this allows the factory to inject itself into the ObjectAccessor.
     */
    interface Aware {
        /**
         * Sets the ObjectAccessorWrapper.
         *
         * @param wrapper the factory to set
         */
        void setWrapper(ObjectAccessorWrapper wrapper);

        /**
         * Returns the ObjectAccessorWrapper.
         *
         * @return the ObjectAccessorWrapper
         */
        ObjectAccessorWrapper getWrapper();
    }

}
