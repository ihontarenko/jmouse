package org.jmouse.core.bind.descriptor;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Represents a descriptor for an element that may have annotations.
 * <p>
 * This interface extends {@link Descriptor} and provides a method to retrieve
 * annotation descriptor associated with the described element.
 * </p>
 *
 * @param <T> the type of the internal representation of the described element
 * @see AnnotationDescriptor
 */
public interface ElementDescriptor<T> extends Descriptor<T> {

    /**
     * Returns a collection of annotation descriptors associated with this element.
     * <p>
     * This method provides access to descriptor about annotations present on the element.
     * </p>
     *
     * @return a collection of {@link AnnotationDescriptor} instances
     */
    Collection<AnnotationDescriptor> getAnnotations();

    /**
     * Retrieves the annotation descriptor for a specific annotation type.
     * <p>
     * If the annotation is present on this element, its corresponding {@link AnnotationDescriptor}
     * will be returned; otherwise, this method returns {@code null}.
     * </p>
     *
     * @param annotation the annotation class to look for
     * @return the corresponding {@link AnnotationDescriptor}, or {@code null} if the annotation is not present
     */
    default AnnotationDescriptor getAnnotation(Class<? extends Annotation> annotation) {
        for (AnnotationDescriptor annotationDescriptor : getAnnotations()) {
            Annotation internal = annotationDescriptor.getInternal();
            if (internal.annotationType().equals(annotation)) {
                return annotationDescriptor;
            }
        }
        return null;
    }

    /**
     * A base implementation of {@link ElementDescriptor} providing support for annotation descriptor.
     * <p>
     * This class extends {@link Descriptor.Implementation} and includes a collection of annotation descriptors.
     * </p>
     *
     * @param <T> the type of the internal representation of the described element
     */
    abstract class Implementation<T> extends Descriptor.Implementation<T> implements ElementDescriptor<T> {

        protected final Set<AnnotationDescriptor> annotations;

        /**
         * Constructs a new {@code ElementDescriptor.Implementation} with the given parameters.
         *
         * @param name        the name of the element
         * @param internal    the internal representation of the element
         * @param annotations a set of annotation descriptors associated with this element
         */
        public Implementation(String name, T internal, Set<AnnotationDescriptor> annotations) {
            super(name, internal);
            this.annotations = annotations;
        }

        /**
         * Returns a collection of annotation descriptors associated with this element.
         *
         * @return a set of {@link AnnotationDescriptor} instances
         */
        @Override
        public Collection<AnnotationDescriptor> getAnnotations() {
            return annotations;
        }
    }

    /**
     * A builder for constructing instances of {@link ElementDescriptor}.
     * <p>
     * This abstract builder provides methods for setting annotation descriptor before
     * finalizing the descriptor instance.
     * </p>
     *
     * @param <B> the type of the builder itself, used for fluent API methods
     * @param <I> the type of the internal representation of the element
     * @param <D> the type of the descriptor being built
     */
    abstract class Builder<B extends Descriptor.Builder<B, I, D>, I, D extends ElementDescriptor<I>>
            extends DescriptorBuilder<B, I, D> {

        protected Set<AnnotationDescriptor> annotations = new HashSet<>();

        /**
         * Constructs a new {@code ElementDescriptor.Builder} with the given name.
         *
         * @param name the name of the element being built
         */
        public Builder(String name) {
            super(name);
        }

        /**
         * Adds an annotation descriptor to the element being built.
         *
         * @param annotation the annotation descriptor to add
         * @return this builder instance for method chaining
         */
        public B annotation(AnnotationDescriptor annotation) {
            this.annotations.add(annotation);
            return self();
        }
    }
}
