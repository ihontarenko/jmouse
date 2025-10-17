package org.jmouse.beans.definition;

import org.jmouse.core.reflection.InferredType;

/**
 * A simple implementation of the {@link BeanDependency} interface.
 * Represents a dependency required by a bean, defined by its type and optional name.
 *
 * @param javaType the type of the dependency.
 * @param name the optional name of the dependency.
 * @param dependant the dependant object.
 */
public record SimpleBeanDependency(InferredType javaType, String name, Object dependant) implements BeanDependency {

}
