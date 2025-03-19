package org.jmouse.core.bind.descriptor.internal;

import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class AnnotationData extends AbstractDataContainer<Annotation> {

    private Map<String, Object> attributes = new HashMap<>();
    private ClassTypeDescriptor annotationType;

    public AnnotationData(Annotation target) {
        super(target);
    }

    public ClassTypeDescriptor getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(ClassTypeDescriptor annotationType) {
        this.annotationType = annotationType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String name, Object value) {
        attributes.put(name, value);
    }

}
