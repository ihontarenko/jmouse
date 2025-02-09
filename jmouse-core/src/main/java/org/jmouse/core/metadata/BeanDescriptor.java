package org.jmouse.core.metadata;

import org.jmouse.common.support.invocable.FieldDescriptor;

import java.util.Collection;

public interface BeanDescriptor extends ElementDescriptor {

    Collection<PropertyDescriptor> getProperties();

    ClassDescriptor getClassType();

    Collection<MethodDescriptor> getMethods();

    Collection<ConstructorDescriptor> getConstructors();

    Collection<FieldDescriptor> getFields();

}
