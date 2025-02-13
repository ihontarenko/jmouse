package org.jmouse.core.bind.descriptor.bean;

/**
 * Defines a contract for accessing and modifying property values of an bean.
 * <p>
 * Implementations of this interface provide methods for injecting and retrieving
 * values from a bean's properties using {@link PropertyDescriptor}.
 * </p>
 *
 * @param <T> the type of the bean whose property is being accessed
 */
public interface PropertyAccessor<T> {

    /**
     * Creates a {@link PropertyAccessor} instance from a {@link PropertyDescriptor}.
     * <p>
     * This method provides a convenient way to create a property accessor that can
     * read and write property values based on the descriptor.
     * </p>
     *
     * @param descriptor the property descriptor that defines how the property is accessed
     * @param <T>        the type of the bean containing the property
     * @return a {@link PropertyAccessor} instance for the specified property
     */
    static <T> PropertyAccessor<T> ofPropertyDescriptor(PropertyDescriptor<T> descriptor) {
        return new Implementation<>(descriptor);
    }

    /**
     * Injects a value into the property of the given bean.
     * <p>
     * If the property is writable, the specified value will be set using the setter
     * method defined in the {@link PropertyDescriptor}.
     * </p>
     *
     * @param object the bean whose property value is being modified
     * @param value  the value to be injected into the property
     */
    void injectValue(T object, Object value);

    /**
     * Retrieves the value of the property from the given bean.
     * <p>
     * If the property is readable, the value is obtained using the getter method
     * defined in the {@link PropertyDescriptor}. If the property is not readable,
     * {@code null} is returned.
     * </p>
     *
     * @param object the bean whose property value is being retrieved
     * @return the value of the property, or {@code null} if it is not readable
     */
    Object obtainValue(T object);

    /**
     * A default implementation of {@link PropertyAccessor} that operates based on
     * a given {@link PropertyDescriptor}.
     * <p>
     * This implementation ensures that only writable properties can be modified,
     * and only readable properties can be accessed.
     * </p>
     *
     * @param <T> the type of the bean whose property is being accessed
     */
    class Implementation<T> implements PropertyAccessor<T> {

        private final PropertyDescriptor<T> descriptor;

        /**
         * Constructs a {@code PropertyAccessor.Implementation} using the given property descriptor.
         *
         * @param descriptor the property descriptor used to access the property
         */
        public Implementation(PropertyDescriptor<T> descriptor) {
            this.descriptor = descriptor;
        }

        /**
         * Injects a value into the bean's property if it is writable.
         * <p>
         * The method first checks if the property is writable before attempting to set the value.
         * </p>
         *
         * @param object the bean whose property value is being modified
         * @param value  the value to be injected into the property
         */
        @Override
        public void injectValue(T object, Object value) {
            if (descriptor.isWritable()) {
                descriptor.getSetter().set(object, value);
            }
        }

        /**
         * Retrieves the value of the bean's property if it is readable.
         * <p>
         * If the property has a getter, the method invokes it to retrieve the value;
         * otherwise, it returns {@code null}.
         * </p>
         *
         * @param object the bean whose property value is being retrieved
         * @return the value of the property, or {@code null} if it is not readable
         */
        @Override
        public Object obtainValue(T object) {
            Object value = null;

            if (descriptor.isReadable()) {
                value = descriptor.getGetter().get(object);
            }

            return value;
        }

        @Override
        public String toString() {
            return "Accessor to: %s".formatted(descriptor);
        }
    }
}
