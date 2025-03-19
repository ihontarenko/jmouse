package org.jmouse.core.bind.descriptor.internal;

import org.jmouse.core.bind.descriptor.AnnotationDescriptor;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

abstract public class AnnotatedElementData<E extends AnnotatedElement> extends AbstractDataContainer<E> {

    private final List<AnnotationDescriptor> annotations = new ArrayList<>();

    public AnnotatedElementData(E target) {
        super(target);
    }

    public List<AnnotationDescriptor> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(final AnnotationDescriptor annotation) {
        annotations.add(annotation);
    }

    public void removeAnnotation(final AnnotationDescriptor annotation) {
        annotations.remove(annotation);
    }

}
