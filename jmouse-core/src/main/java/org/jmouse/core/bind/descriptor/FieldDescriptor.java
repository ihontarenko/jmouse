package org.jmouse.core.bind.descriptor;

import org.jmouse.core.reflection.Reflections;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

/**
 * Represents a descriptor for a field in a class.
 * <p>
 * This interface extends {@link ElementDescriptor} and {@link ClassTypeInspector},
 * providing descriptor about the field, including its type and annotations.
 * </p>
 *
 * @see ElementDescriptor
 * @see ClassTypeInspector
 * @see TypeDescriptor
 * @see AnnotationDescriptor
 */
public interface FieldDescriptor extends ElementDescriptor<Field>, ClassTypeInspector {

    /**
     * Returns the type descriptor of this field.
     * <p>
     * This method provides access to the {@link TypeDescriptor} representing
     * the type of the field.
     * </p>
     *
     * @return the {@link TypeDescriptor} representing the field type
     */
    TypeDescriptor getType();

    /**
     * Default implementation of {@link FieldDescriptor}.
     * <p>
     * This class provides a concrete implementation for storing descriptor related to fields,
     * including their annotations and type.
     * </p>
     */
    class Implementation extends ElementDescriptor.Implementation<Field> implements FieldDescriptor {

        private final TypeDescriptor type;

        /**
         * Constructs a new {@code FieldDescriptor.Implementation} instance.
         *
         * @param name        the name of the field
         * @param internal    the underlying {@link Field} instance
         * @param annotations a set of annotation descriptors associated with this field
         * @param type        the descriptor representing the type of this field
         */
        Implementation(String name, Field internal, Set<AnnotationDescriptor> annotations, TypeDescriptor type) {
            super(name, internal, annotations);
            this.type = type;
        }

        /**
         * Returns the class type being inspected.
         * <p>
         * This method delegates to {@link TypeDescriptor#getClassType()} to retrieve
         * the underlying {@link Class} of this field.
         * </p>
         *
         * @return the {@link Class} bean representing the inspected field type
         */
        @Override
        public Class<?> getClassType() {
            return getType().getClassType();
        }

        /**
         * Returns the type descriptor of this field.
         *
         * @return the {@link TypeDescriptor} representing the field type
         */
        @Override
        public TypeDescriptor getType() {
            return type;
        }

        @Override
        public String toString() {
            return Reflections.getFieldName(internal);
        }
    }

    /**
     * A builder for constructing instances of {@link FieldDescriptor}.
     * <p>
     * This builder provides a fluent API for setting field descriptor before
     * creating an immutable {@link FieldDescriptor} instance.
     * </p>
     */
    class Builder extends ElementDescriptor.Builder<Builder, Field, FieldDescriptor> {

        private TypeDescriptor type;

        /**
         * Constructs a new {@code FieldDescriptor.Builder} with the specified field name.
         *
         * @param name the name of the field
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Sets the type of the field being built.
         *
         * @param type the {@link TypeDescriptor} representing the field type
         * @return this builder instance for method chaining
         */
        public Builder type(TypeDescriptor type) {
            this.type = type;
            return self();
        }

        /**
         * Builds a new {@link FieldDescriptor} instance using the configured values.
         * <p>
         * The resulting descriptor is immutable, ensuring thread-safety.
         * </p>
         *
         * @return a new immutable instance of {@link FieldDescriptor}
         */
        @Override
        public FieldDescriptor build() {
            return new Implementation(name, internal, Collections.unmodifiableSet(annotations), type);
        }
    }

    /**
     * Creates a descriptor for a field.
     *
     * @param field the field to describe
     * @return a {@link FieldDescriptor} instance representing the field
     */
    static FieldDescriptor forField(Field field) {
        return forField(field, TypeDescriptor.DEFAULT_DEPTH);
    }

    /**
     * Creates a descriptor for a field with a specified depth for nested elements.
     *
     * @param field the field to describe
     * @param depth the recursion depth limit for nested descriptor resolution
     * @return a {@link FieldDescriptor} instance
     */
    static FieldDescriptor forField(Field field, int depth) {
        FieldDescriptor.Builder builder = new FieldDescriptor.Builder(field.getName());

        builder.internal(field).type(TypeDescriptor.forClass(field.getType(), depth - 1));

        for (Annotation annotation : field.getAnnotations()) {
            builder.annotation(AnnotationDescriptor.forAnnotation(annotation, depth - 1));
        }

        return builder.build();
    }
}
