package org.jmouse.core.metadata.object;

import org.jmouse.core.metadata.AnnotationDescriptor;
import org.jmouse.core.metadata.ClassDescriptor;
import org.jmouse.core.metadata.ClassTypeInspector;
import org.jmouse.core.metadata.ElementDescriptor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents metadata for an object, including its properties and annotations.
 * <p>
 * This interface extends {@link ElementDescriptor} and {@link ClassTypeInspector},
 * allowing introspection of an object's structure, including its properties and type.
 * </p>
 *
 * @param <T> the type of the object being described
 */
public interface ObjectDescriptor<T> extends ElementDescriptor<T>, ClassTypeInspector {

    /**
     * Returns a collection of all properties associated with this object.
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
     * Checks whether the object contains a property with the given name.
     *
     * @param name the name of the property
     * @return {@code true} if the property exists, otherwise {@code false}
     */
    boolean hasProperty(String name);

    /**
     * A default implementation of {@link ObjectDescriptor}.
     * <p>
     * This implementation provides access to an object's properties and metadata,
     * including its annotations and type information.
     * </p>
     *
     * @param <T> the type of the object being described
     */
    abstract class Implementation<T> extends ElementDescriptor.Implementation<T> implements ObjectDescriptor<T> {

        protected final ClassDescriptor                              type;
        protected final Map<String, ? extends PropertyDescriptor<T>> properties;

        /**
         * Constructs a new {@code ObjectDescriptor.Implementation}.
         *
         * @param name        the name of the object
         * @param internal    the internal representation of the object
         * @param annotations the annotations associated with this object
         * @param type        the class descriptor representing the object's type
         * @param properties  the properties associated with this object
         */
        Implementation(
                String name,
                T internal,
                Set<AnnotationDescriptor> annotations,
                ClassDescriptor type,
                Map<String, ? extends PropertyDescriptor<T>> properties
        ) {
            super(name, internal, annotations);
            this.type = type;
            this.properties = properties;
        }

        /**
         * Returns a collection of all properties associated with this object.
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
         * Checks whether the object contains a property with the given name.
         *
         * @param name the name of the property
         * @return {@code true} if the property exists, otherwise {@code false}
         */
        @Override
        public boolean hasProperty(String name) {
            return properties.containsKey(name);
        }

        /**
         * Returns a string representation of this object descriptor.
         *
         * @return a string representing the class type of this object
         */
        @Override
        public String toString() {
            return type.toString();
        }
    }

    /**
     * A builder class for constructing {@link ObjectDescriptor} instances.
     *
     * @param <T> the type of the object being described
     */
    abstract class Builder<T> extends ElementDescriptor.Builder<ObjectDescriptor.Builder<T>, T, ObjectDescriptor<T>> {

        protected Map<String, PropertyDescriptor<T>> properties = new LinkedHashMap<>();
        protected T                                  bean;
        protected ClassDescriptor                    descriptor;

        /**
         * Constructs a new {@code ObjectDescriptor.Builder}.
         *
         * @param name the name of the object being built
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the object instance associated with this descriptor.
         *
         * @param bean the object instance
         * @return this builder instance
         */
        public ObjectDescriptor.Builder<T> bean(T bean) {
            this.bean = bean;
            return internal(bean).self();
        }

        /**
         * Sets the class descriptor representing the object's type.
         *
         * @param descriptor the {@link ClassDescriptor} representing the object's type
         * @return this builder instance
         */
        public ObjectDescriptor.Builder<T> descriptor(ClassDescriptor descriptor) {
            this.descriptor = descriptor;
            return self();
        }

        /**
         * Sets the properties associated with this object descriptor.
         *
         * @param properties a map of property names to {@link PropertyDescriptor} instances
         * @return this builder instance
         */
        public ObjectDescriptor.Builder<T> properties(Map<String, PropertyDescriptor<T>> properties) {
            this.properties = properties;
            return self();
        }

        /**
         * Adds a single property descriptor to this object descriptor.
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
