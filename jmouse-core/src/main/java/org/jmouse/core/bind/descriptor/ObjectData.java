package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;
import org.jmouse.core.bind.introspection.internal.AbstractDataContainer;
import org.jmouse.core.reflection.JavaType;

public class ObjectData<T> extends AbstractDataContainer<T> {

    private ClassTypeDescriptor type;

    public ObjectData(T target) {
        super(target);
    }

    public ClassTypeDescriptor getType() {
        return type;
    }

    public void setType(ClassTypeDescriptor type) {
        this.type = type;
    }
}
