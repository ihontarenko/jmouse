package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.AnnotationData;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;

public class AnnotationDescriptor extends AbstractDescriptor<Annotation, AnnotationData, AnnotationIntrospector> {

    protected AnnotationDescriptor(AnnotationIntrospector introspector, AnnotationData container) {
        super(introspector, container);
    }

    public ClassTypeDescriptor getType() {
        return container.getAnnotationType();
    }

    public Object getAttribute(String attribute) {
        return container.getAttribute(attribute);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(container.getAttributes());
    }

    @Override
    public AnnotationIntrospector toIntrospector() {
        return introspector;
    }

}
