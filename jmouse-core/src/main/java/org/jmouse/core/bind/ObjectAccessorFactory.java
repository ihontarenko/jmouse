package org.jmouse.core.bind;

/**
 * A factory interface for creating ObjectAccessor instances.
 */
public interface ObjectAccessorFactory {
    /**
     * Wraps the given source object into an ObjectAccessor.
     *
     * @param source the source object to wrap
     * @return an ObjectAccessor instance
     */
    ObjectAccessor wrap(Object source);

    /**
     * An interface for ObjectAccessors that require awareness of the factory.
     * Implementing this allows the factory to inject itself into the ObjectAccessor.
     */
    interface Aware {
        /**
         * Sets the ObjectAccessorFactory.
         *
         * @param factory the factory to set
         */
        void setFactory(ObjectAccessorFactory factory);

        /**
         * Returns the ObjectAccessorFactory.
         *
         * @return the ObjectAccessorFactory
         */
        ObjectAccessorFactory getFactory();
    }

}
