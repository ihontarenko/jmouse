package org.jmouse.core.bind.descriptor.bean;

import org.jmouse.core.bind.PropertyAccessor;
import org.jmouse.core.bind.descriptor.AnnotationDescriptor;
import org.jmouse.core.bind.descriptor.TypeDescriptor;
import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.bind.descriptor.ElementDescriptor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents descriptor for an bean, including its properties and annotations.
 * <p>
 * This interface extends {@link ElementDescriptor} and {@link ClassTypeInspector},
 * allowing introspection of an bean's structure, including its properties and type.
 * </p>
 *
 * @param <T> the type of the bean being described
 */
public interface ObjectDescriptor<T> extends ElementDescriptor<T>, ClassTypeInspector {

    /**
     * Returns a collection of all properties associated with this bean.
     * <p>
     * Each property in the collection is represented by a {@link PropertyDescriptor}.
     * </p>
     *
     * @return a collection of property descriptors
     */
    Collection<? extends PropertyDescriptor<T>> getProperties();

    /**
     * Retrieves a property descriptor by its name.
     * <p>
     * If no property with the given name exists, this method returns {@code null}.
     * </p>
     *
     * @param name the name of the property
     * @return the property descriptor, or {@code null} if not found
     */
    PropertyDescriptor<T> getProperty(String name);

    /**
     * Retrieves a property accessor by its name.
     * <p>
     * If no property with the given name exists, this method returns {@code null}.
     * </p>
     *
     * @param name the name of the property
     * @return the property accessor, or {@code null} if not found
     */
    PropertyAccessor<T> getPropertyAccessor(String name);

    /**
     * Checks whether the bean contains a property with the given name.
     *
     * @param name the name of the property
     * @return {@code true} if the property exists, otherwise {@code false}
     */
    boolean hasProperty(String name);

    /**
     * A default implementation of {@link ObjectDescriptor}.
     * <p>
     * This implementation provides access to an bean's properties and descriptor,
     * including its annotations and type information.
     * </p>
     *
     * @param <T> the type of the bean being described
     */
    abstract class Implementation<T> extends ElementDescriptor.Implementation<T> implements ObjectDescriptor<T> {

        protected final TypeDescriptor                               type;
        protected final Map<String, ? extends PropertyDescriptor<T>> properties;

        /**
         * Constructs a new {@code ObjectDescriptor.PropertyDescriptorAccessor}.
         *
         * @param name        the name of the bean
         * @param internal    the internal representation of the bean
         * @param annotations the annotations associated with this bean
         * @param type        the class descriptor representing the bean's type
         * @param properties  the properties associated with this bean
         */
        Implementation(
                String name,
                T internal,
                Set<AnnotationDescriptor> annotations,
                TypeDescriptor type,
                Map<String, ? extends PropertyDescriptor<T>> properties
        ) {
            super(name, internal, annotations);

            this.type = type;
            this.properties = properties;
        }

        /**
         * Returns a collection of all properties associated with this bean.
         * <p>
         * Each property in the collection is represented by a {@link PropertyDescriptor}.
         * </p>
         *
         * @return a collection of property descriptors
         */
        @Override
        public Collection<? extends PropertyDescriptor<T>> getProperties() {
            return properties.values();
        }

        /**
         * Retrieves a property descriptor by its name.
         * <p>
         * If no property with the given name exists, this method returns {@code null}.
         * </p>
         *
         * @param name the name of the property
         * @return the property descriptor, or {@code null} if not found
         */
        @Override
        public PropertyDescriptor<T> getProperty(String name) {
            return properties.get(name);
        }

        /**
         * Retrieves a property accessor by its name.
         * <p>
         * If no property with the given name exists, this method returns {@code null}.
         * </p>
         *
         * @param name the name of the property
         * @return the property accessor, or {@code null} if not found
         */
        @Override
        public PropertyAccessor<T> getPropertyAccessor(String name) {
            PropertyAccessor<T> accessor = null;

            if (hasProperty(name)) {
                accessor = getProperty(name).getPropertyAccessor();
            }

            return accessor;
        }

        /**
         * Checks whether the bean contains a property with the given name.
         *
         * @param name the name of the property
         * @return {@code true} if the property exists, otherwise {@code false}
         */
        @Override
        public boolean hasProperty(String name) {
            return properties.containsKey(name);
        }

        /**
         * Returns a string representation of this bean descriptor.
         *
         * @return a string representing the class type of this bean
         */
        @Override
        public String toString() {
            return type.toString();
        }
    }

    /**
     * A builder class for constructing {@link ObjectDescriptor} instances.
     *
     * @param <T> the type of the bean being described
     */
    abstract class Builder<T> extends Mutable<Builder<T>, T, ObjectDescriptor<T>> {

        protected Map<String, PropertyDescriptor<T>> properties = new LinkedHashMap<>();
        protected T                                  bean;
        protected TypeDescriptor                     descriptor;

        /**
         * Constructs a new {@code ObjectDescriptor.Builder}.
         *
         * @param name the name of the bean being built
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the bean instance associated with this descriptor.
         *
         * @param bean the bean instance
         * @return this builder instance
         */
        public ObjectDescriptor.Builder<T> bean(T bean) {
            this.bean = bean;
            return target(bean).self();
        }

        /**
         * Sets the class descriptor representing the bean's type.
         *
         * @param descriptor the {@link TypeDescriptor} representing the bean's type
         * @return this builder instance
         */
        public ObjectDescriptor.Builder<T> descriptor(TypeDescriptor descriptor) {
            this.descriptor = descriptor;
            return self();
        }

        /**
         * Sets the properties associated with this bean descriptor.
         *
         * @param properties a map of property names to {@link PropertyDescriptor} instances
         * @return this builder instance
         */
        public ObjectDescriptor.Builder<T> properties(Map<String, PropertyDescriptor<T>> properties) {
            this.properties = properties;
            return self();
        }

        /**
         * Adds a single property descriptor to this bean descriptor.
         *
         * @param property the {@link PropertyDescriptor} to add
         * @return this builder instance
         */
        public ObjectDescriptor.Builder<T> property(PropertyDescriptor<T> property) {
            this.properties.put(property.getName(), property);
            return self();
        }

    }
}
