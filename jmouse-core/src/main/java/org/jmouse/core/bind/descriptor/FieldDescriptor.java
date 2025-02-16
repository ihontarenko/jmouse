package org.jmouse.core.bind.descriptor;

import org.jmouse.core.reflection.ClassTypeInspector;
import org.jmouse.core.reflection.JavaType;
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

    static Mutable composer(Field field) {
        return new Mutable(field);
    }

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

        public Implementation(
                Descriptor.Mutable<?, Field, Descriptor<Field>> mutable,
                String name,
                Field internal,
                Set<AnnotationDescriptor> annotations,
                TypeDescriptor type
        ) {
            super(mutable, name, internal, annotations);
            this.type = type;
        }

//
//        Implementation(String name, Field internal, Set<AnnotationDescriptor> annotations, TypeDescriptor type) {
//            super(name, internal, annotations);
//            this.type = type;
//        }

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
            return Reflections.getFieldName(target);
        }

        @Override
        public Descriptor<Field> introspect() {
            return null;
        }
    }

    /**
     * A builder for constructing instances of {@link FieldDescriptor}.
     * <p>
     * This builder provides a fluent API for setting field descriptor before
     * creating an immutable {@link FieldDescriptor} instance.
     * </p>
     */
    class Mutable extends ElementDescriptor.Mutable<Mutable, Field, FieldDescriptor> {

        private TypeDescriptor type;

        /**
         * Constructs a new {@code ElementDescriptor.Builder} with the given name.
         *
         * @param element the name of the element being built
         */
        public Mutable(Field element) {
            super(element);
        }

        /**
         * Sets the type of the field being built.
         *
         * @param type the {@link TypeDescriptor} representing the field type
         * @return this builder instance for method chaining
         */
        public Mutable type(TypeDescriptor type) {
            this.type = type;
            return self();
        }

        public Mutable type() {
            return type(TypeDescriptor.builder(target.getType()).toImmutable());
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
        public FieldDescriptor toImmutable() {
            return new Implementation(
                    this,
                    name,
                    target,
                    Collections.unmodifiableSet(annotations)
            );
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
        Mutable builder = new Mutable(field.getName());

        builder.target(field).type(TypeDescriptor.forType(JavaType.forField(field), depth - 1));

        for (Annotation annotation : field.getAnnotations()) {
            builder.annotation(AnnotationDescriptor.forAnnotation(annotation, depth - 1));
        }

        return builder.toImmutable();
    }
}
