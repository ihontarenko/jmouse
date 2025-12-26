package org.jmouse.core.bind.descriptor.structured;

import org.jmouse.core.Contract;
import org.jmouse.core.bind.DirectPropertyAccess;
import org.jmouse.core.bind.PropertyAccessor;
import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;

import java.util.Map;

/**
 * Represents a descriptor for an object that provides structured metadata about its properties.
 * <p>
 * This interface defines methods for accessing and managing properties of an object,
 * including retrieving type information and property accessors.
 * </p>
 *
 * @param <T> the type of the object being described
 *
 * @author Mr. Jerry Mouse
 * @author Ivan Hontarenko
 */
public interface ObjectDescriptor<T> {

    /**
     * Retrieves the {@link PropertyAccessor} associated with this object.
     * <p>
     * A property accessor provides methods for injecting and retrieving values from the object.
     * Implementations may override this method to return a valid accessor.
     * </p>
     *
     * @return a {@link PropertyAccessor} instance, or {@code null} if not provided
     */
    default PropertyAccessor<T> getDefaultAccessor(String propertyName) {
        return DirectPropertyAccess.forPropertyDescriptor(getProperty(propertyName));
    }

    /**
     * Retrieves the type descriptor of this object.
     * <p>
     * The type descriptor contains metadata about the class type of the described object.
     * </p>
     *
     * @return the {@link ClassTypeDescriptor} representing the type of this object
     */
    ClassTypeDescriptor getType();

    /**
     * Sets the type descriptor of this object.
     *
     * @param type the {@link ClassTypeDescriptor} representing the new type
     */
    void setType(ClassTypeDescriptor type);

    /**
     * Retrieves a map of property descriptors associated with this object.
     * <p>
     * Each entry in the map represents a named property and its corresponding descriptor.
     * </p>
     *
     * @return a map of property names to {@link PropertyDescriptor} instances
     */
    Map<String, PropertyDescriptor<T>> getProperties();

    /**
     * Checks if this object has a property with the specified name.
     *
     * @param name the name of the property
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    default boolean hasProperty(String name) {
        return getProperties().containsKey(name);
    }

    /**
     * Retrieves a property descriptor by name.
     *
     * @param name the name of the property
     * @return the {@link PropertyDescriptor} associated with the given name, or {@code null} if not found
     */
    default PropertyDescriptor<T> getProperty(String name) {
        return getProperties().get(name);
    }

    /**
     * Adds a new property descriptor to this object.
     * <p>
     * If a property with the same name already exists, it will be replaced.
     * </p>
     *
     * @param property the {@link PropertyDescriptor} to add
     */
    default void addProperty(PropertyDescriptor<T> property) {
        getProperties().put(property.getName(), property);
    }

    /**
     * Obtain the value of the specified property from the given instance.
     * <p>
     * The property must exist and be readable; otherwise an
     * {@link IllegalArgumentException} is raised.
     * </p>
     *
     * @param property the property name
     * @param instance the target object instance
     * @return the current value of the property
     */
    default Object obtainValue(String property, T instance) {
        Contract.state(hasProperty(property), () -> new IllegalArgumentException(
                "Property " + property + " does not exist"));
        PropertyAccessor<T> accessor = getProperty(property).getAccessor();
        Contract.state(accessor.isReadable(), () -> new IllegalArgumentException(
                "Property " + property + " is not readable"));
        return accessor.readValue(instance);
    }

    /**
     * Inject the given value into the specified property of the given instance.
     * <p>
     * The property must exist and be writable; otherwise an
     * {@link IllegalArgumentException} is raised.
     * </p>
     *
     * @param property the property name
     * @param instance the target object instance
     * @param value    the value to inject
     */
    default void injectValue(String property, T instance, Object value) {
        Contract.state(hasProperty(property), () -> new IllegalArgumentException(
                "Property " + property + " does not exist"));
        PropertyAccessor<T> accessor = getProperty(property).getAccessor();
        Contract.state(accessor.isWritable(), () -> new IllegalArgumentException(
                "Property " + property + " is not writable"));
        accessor.writeValue(instance, value);
    }

}
