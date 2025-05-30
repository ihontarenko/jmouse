package org.jmouse.beans.definition;

import org.jmouse.core.reflection.JavaType;

public record AggregatedBeansDependency(JavaType javaType, String name, Object dependant) implements BeanDependency {

}
