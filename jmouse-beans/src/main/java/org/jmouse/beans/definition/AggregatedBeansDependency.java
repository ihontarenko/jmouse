package org.jmouse.beans.definition;

import org.jmouse.core.reflection.InferredType;

public record AggregatedBeansDependency(InferredType javaType, String name, Object dependant) implements BeanDependency {

}
