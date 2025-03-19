package org.jmouse.core.bind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    void register(Class<?> instanceType, String name, VirtualProperty<?> property);

    /**
     * Registers a virtual property using the instance type obtained from the property.
     *
     * @param name     the name of the virtual property
     * @param property the VirtualProperty definition which contains its own instance type information
     */
    default void register(String name, VirtualProperty<?> property) {
        register(property.getInstanceType(), name, property);
    }

    /**
     * Resolves the virtual property for the given instance and property name.
     * <p>
     * The resolver checks all registered virtual properties for the object's class and returns the first non-null
     * value obtained by calling {@code getValue} on the property.
     * </p>
     *
     * @param instance the object instance from which to resolve the virtual property
     * @param name     the name of the virtual property to resolve
     * @return the resolved value of the virtual property, or {@code null} if none of the registered properties
     * return a non-null value
     */
    Object resolve(Object instance, String name);

    /**
     * An interface that allows an object to be aware of its VirtualPropertyResolver.
     */
    interface Aware {
        /**
         * Returns the currently set VirtualPropertyResolver.
         *
         * @return the VirtualPropertyResolver instance
         */
        VirtualPropertyResolver getVirtualPropertyResolver();

        /**
         * Sets the VirtualPropertyResolver.
         *
         * @param resolver the VirtualPropertyResolver instance to set
         */
        void setVirtualPropertyResolver(VirtualPropertyResolver resolver);
    }

    /**
     * The default implementation of {@code VirtualPropertyResolver}.
     * <p>
     * This implementation maintains a thread-safe registry mapping instance types and property names
     * to lists of {@link VirtualProperty} definitions.
     * </p>
     */
    class Default implements VirtualPropertyResolver {

        private final Map<Class<?>, Map<String, List<VirtualProperty<?>>>> properties = new ConcurrentHashMap<>();

        /**
         * Registers a virtual property for a given instance type and property name.
         *
         * @param instanceType the class of the object to which the virtual property applies
         * @param name         the name of the virtual property
         * @param property     the VirtualProperty definition
         */
        @Override
        public void register(Class<?> instanceType, String name, VirtualProperty<?> property) {
            var names = properties.computeIfAbsent(instanceType, k -> new ConcurrentHashMap<>());
            names.computeIfAbsent(name, k -> new ArrayList<>()).add(property);
        }

        /**
         * Resolves the virtual property for the specified instance and property name.
         * <p>
         * It retrieves all registered virtual properties for the instance's class and property name,
         * iterates over them, and returns the first non-null value provided by the {@code getValue} method.
         * </p>
         *
         * @param instance the object instance from which to resolve the property
         * @param name     the name of the virtual property
         * @return the resolved property value, or {@code null} if no virtual property returns a non-null value
         */
        @Override
        @SuppressWarnings({"unchecked"})
        public Object resolve(Object instance, String name) {
            List<VirtualProperty<?>> properties = getProperties(instance.getClass(), name);
            Object                   value      = null;

            for (VirtualProperty<?> property : properties) {
                value = ((VirtualProperty<Object>) property).getValue(instance, name);
                if (value != null) {
                    break;
                }
            }

            return value;
        }

        /**
         * Retrieves the list of virtual properties registered for the specified instance type and property name.
         *
         * @param instanceType the class of the object
         * @param name         the property name
         * @return a list of VirtualProperty instances; returns an empty list if none are registered
         */
        protected List<VirtualProperty<?>> getProperties(Class<?> instanceType, String name) {
            List<VirtualProperty<?>> result = Collections.emptyList();

            if (properties.containsKey(instanceType)) {
                Map<String, List<VirtualProperty<?>>> names = properties.get(instanceType);
                if (names.containsKey(name)) {
                    result = names.get(name);
                }
            }

            return result;
        }
    }
}
