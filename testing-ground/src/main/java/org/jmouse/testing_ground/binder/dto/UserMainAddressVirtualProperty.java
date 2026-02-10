package org.jmouse.testing_ground.binder.dto;

import org.jmouse.core.access.VirtualProperty;

public class UserMainAddressVirtualProperty implements VirtualProperty<User> {

    /**
     * Returns the instance type for which this virtual property is applicable.
     *
     * @return the {@link Class} representing the type T
     */
    @Override
    public Class<User> getType() {
        return User.class;
    }

    /**
     * Returns the name of this property.
     *
     * @return the property name
     */
    @Override
    public String getName() {
        return "mainAddress";
    }

    /**
     * Injects a value into the specified property of the given object.
     * <p>
     * This method sets a new value for a property if the property is writable.
     * </p>
     *
     * @param object the target object whose property will be modified
     * @param value  the value to inject into the property
     * @throws IllegalArgumentException if the property is not writable
     */
    @Override
    public void writeValue(User object, Object value) {

    }

    /**
     * Retrieves the current value of the specified property from the given object.
     * <p>
     * If the property is readable, this method returns its current value;
     * otherwise, it may return {@code null}.
     * </p>
     *
     * @param object the target object from which to retrieve the property value
     * @return the value of the property, or {@code null} if it cannot be accessed
     */
    @Override
    public Object readValue(User object) {
        Address address = object.getAddress().getFirst();
        return STR."\{address.getStreet()}, \{address.getCity()}";
    }

    /**
     * Checks if the property is readable (i.e., has a getter).
     *
     * @return {@code true} if the property has a getter, {@code false} otherwise
     */
    @Override
    public boolean isReadable() {
        return true;
    }

}
