package org.jmouse.web.binding.validation;

import java.lang.annotation.Annotation;

public interface ValidationAnnotationReader {

    /**
     * @param destinationDescriptor descriptor object from MappingDestination (may be null)
     * @return annotation instance, if present
     */
    <A extends Annotation> A findOnDestination(Object destinationDescriptor, Class<A> annotationType);

    /**
     * @param targetClass mapped target class
     */
    <A extends Annotation> A findOnTargetClass(Class<?> targetClass, Class<A> annotationType);
}
