package org.jmouse.core.bind;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines an interface for resolving virtual properties for a given object instance.
 * <p>
 * Virtual properties are properties that do not exist as actual fields on the object but can be computed,
 * derived, or provided via external logic.
 * </p>
 */
public interface VirtualPropertyResolver {

    /**
     * Returns the default implementation of the {@code VirtualPropertyResolver}.
     *
     * @return a default VirtualPropertyResolver instance
     */
    static VirtualPropertyResolver defaultResolver() {
        return new Default();
    }

    /**
     * Registers a virtual property for the specified instance type and property name.
     *
     * @param instanceType the class of the object to which the virtual property applies
     * @param name         the name of the virtual property
     * @param property     the VirtualProperty definition
     */
    void addVirtualProperty(Class<?> instanceType, String name, VirtualProperty<?> property);

    /**
     * Registers a virtual property using the instance type obtained from the property.
     *
     * @param property the VirtualProperty definition which contains its own instance type information and property name
     */
    default void addVirtualProperty(VirtualProperty<?> property) {
        addVirtualProperty(property.getType(), property.getName(), property);
    }

    /**
     * Resolves the virtual property for the given instance and property name.
     * <p>
     * The resolver checks the registered virtual property for the object's class and returns the associated
     * VirtualProperty if one is found.
     * </p>
     *
     * @param instance the object instance from which to resolve the virtual property
     * @param name     the name of the virtual property to resolve
     * @return the registered VirtualProperty for the given instance type and name, or {@code null} if none is found
     */
    VirtualProperty<?> getVirtualProperty(Object instance, String name);

    /**
     * An interface that allows an object to be aware of its VirtualPropertyResolver.
     */
    interface Aware {
        /**
         * Returns the currently set VirtualPropertyResolver.
         *
         * @return the VirtualPropertyResolver instance
         */
        VirtualPropertyResolver getVirtualProperties();

        /**
         * Sets the VirtualPropertyResolver.
         *
         * @param resolver the VirtualPropertyResolver instance to set
         */
        void setVirtualProperties(VirtualPropertyResolver resolver);
    }

    /**
     * The default implementation of {@code VirtualPropertyResolver}.
     * <p>
     * This implementation maintains a thread-safe registry that maps instance types and property names
     * to VirtualProperty definitions.
     * </p>
     */
    class Default implements VirtualPropertyResolver {

        private final Map<Class<?>, Map<String, VirtualProperty<?>>> properties = new ConcurrentHashMap<>();

        /**
         * Registers a virtual property for a given instance type and property name.
         *
         * @param instanceType the class of the object to which the virtual property applies
         * @param name         the name of the virtual property
         * @param property     the VirtualProperty definition
         */
        @Override
        public void addVirtualProperty(Class<?> instanceType, String name, VirtualProperty<?> property) {
            properties.computeIfAbsent(instanceType, k -> new ConcurrentHashMap<>())
                    .put(name, property);
        }

        /**
         * Resolves the virtual property for the specified instance and property name.
         * <p>
         * It retrieves the VirtualProperty registered for the instance's class and the given property name.
         * </p>
         *
         * @param instance the object instance from which to resolve the property
         * @param name     the name of the virtual property
         * @return the registered VirtualProperty, or {@code null} if none is found
         */
        @Override
        public VirtualProperty<?> getVirtualProperty(Object instance, String name) {
            Map<String, VirtualProperty<?>> names = properties.get(instance.getClass());

            if (names == null) {
                for (Class<?> type : properties.keySet()) {
                    if (type.isAssignableFrom(instance.getClass())) {
                        names = properties.get(type);
                    }
                }
            }

            return names != null ? names.get(name) : null;
        }

    }
}
