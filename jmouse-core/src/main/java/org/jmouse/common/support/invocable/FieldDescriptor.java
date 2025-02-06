package org.jmouse.common.support.invocable;

import java.lang.reflect.Field;

public interface FieldDescriptor extends TypeDescriptor {

    ClassTypeDescriptor getObjectDescriptor();

    Field getNativeField();

    Class<?> getFieldType();

    ClassTypeDescriptor getFieldTypeDescriptor();

}
