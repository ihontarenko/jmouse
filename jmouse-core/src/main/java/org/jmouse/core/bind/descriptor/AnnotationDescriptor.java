package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.internal.AnnotationData;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;

/**
 * 📝 Descriptor wrapping an {@link Annotation} instance with introspection support.
 * <p>
 * Provides access to annotation type, its attribute values, and an introspector
 * to analyze or manipulate the annotation metadata.
 * </p>
 *
 * <p>Use this class to read annotation attributes in a type-safe manner
 * and to integrate with the binding/introspection framework.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AnnotationDescriptor extends AbstractDescriptor<Annotation, AnnotationData, AnnotationIntrospector> {

    /**
     * Constructs an AnnotationDescriptor with given introspector and container data.
     *
     * @param introspector introspector responsible for annotation analysis
     * @param container   internal data container representing the annotation instance
     */
    protected AnnotationDescriptor(AnnotationIntrospector introspector, AnnotationData container) {
        super(introspector, container);
    }

    /**
     * 🔍 Returns the type descriptor of the annotation.
     *
     * @return descriptor for the annotation's class type
     */
    public ClassTypeDescriptor getType() {
        return container.getAnnotationType();
    }

    /**
     * 🗝️ Gets the value of a specific annotation attribute by name.
     *
     * @param attribute attribute name (method name in annotation)
     * @return value of the attribute, or null if not present
     */
    public Object getAttribute(String attribute) {
        return container.getAttribute(attribute);
    }

    /**
     * 🗃️ Returns an unmodifiable map of all annotation attributes and their values.
     *
     * @return map of attribute names to values
     */
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(container.getAttributes());
    }

    /**
     * 🔧 Returns the underlying {@link AnnotationIntrospector} instance.
     *
     * @return introspector used for this descriptor
     */
    @Override
    public AnnotationIntrospector toIntrospector() {
        return introspector;
    }

}
