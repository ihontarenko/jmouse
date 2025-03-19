package org.jmouse.core.bind.descriptor.internal;

import org.jmouse.core.bind.descriptor.ClassTypeDescriptor;

import java.lang.reflect.Field;

public class FieldData extends AnnotatedElementData<Field> {

    private ClassTypeDescriptor type;

    public FieldData(Field target) {
        super(target);
    }

    public ClassTypeDescriptor getType() {
        return type;
    }

    public void setType(ClassTypeDescriptor type) {
        this.type = type;
    }

}
