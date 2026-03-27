package org.jmouse.beans.definition;

import org.jmouse.core.reflection.InferredType;

import java.lang.reflect.AnnotatedElement;

public record AggregatedBeansDependency(
        InferredType javaType, String name, AnnotatedElement dependant) implements BeanDependency {

}
