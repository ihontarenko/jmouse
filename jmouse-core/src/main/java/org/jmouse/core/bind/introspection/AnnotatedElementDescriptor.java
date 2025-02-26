package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.AnnotatedElementData;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collections;
import java.util.List;

/**
 * Represents a descriptor for annotated Java elements, such as classes, methods, fields, or constructors.
 *
 * <p>This descriptor provides convenient access to annotations present on the annotated element.</p>
 *
 * <pre>{@code
 * AnnotatedElementDescriptor<?, ?, ?> descriptor = ...;
 * AnnotationDescriptor myAnnotation = descriptor.findAnnotation(MyAnnotation.class);
 * }</pre>
 *
 * @param <E> the type of the annotated element (class, method, field, etc.)
 * @param <C> the container type holding introspection data
 * @param <I> the introspector type
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public abstract class AnnotatedElementDescriptor<E extends AnnotatedElement, C extends AnnotatedElementData<E>, I extends AnnotatedElementIntrospector<?, ?, ?, ?>>
        extends AbstractDescriptor<E, C, I> {

    protected AnnotatedElementDescriptor(I introspector, C container) {
        super(introspector, container);
    }

    /**
     * Retrieves an immutable list of annotation descriptors associated with this element.
     *
     * @return an unmodifiable list of annotations
     */
    public List<AnnotationDescriptor> getAnnotations() {
        return Collections.unmodifiableList(container.getAnnotations());
    }

    /**
     * Finds a specific annotation descriptor by its annotation type.
     *
     * @param annotationType the class of the annotation to search for
     * @return the found annotation descriptor, or {@code null} if not present
     */
    public AnnotationDescriptor findAnnotation(final Class<? extends Annotation> annotationType) {
        for (AnnotationDescriptor annotation : getAnnotations()) {
            if (annotation.getType().is(annotationType)) {
                return annotation;
            }
        }
        return null;
    }
}
