package org.jmouse.beans.definition;

import org.jmouse.core.reflection.TypeInfer;

public record AggregatedBeansDependency(TypeInfer javaType, String name, Object dependant) implements BeanDependency {

}
