package org.jmouse.core.bind;

import org.jmouse.core.bind.descriptor.ConstructorDescriptor;
import org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.util.Exceptions;

import static org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor.forBean;
import static org.jmouse.core.bind.descriptor.bean.JavaBeanDescriptor.forValueObject;

public class ImmutableBean<T> {

    private final JavaType type;

    private ImmutableBean(JavaType type) {
        this.type = type;
        lookupConstructor(type.isRecord() ? forValueObject(type.getRawType()) : forBean(type.getRawType()));
    }

    private ImmutableBean(Class<T> clazz) {
        this(JavaType.forClass(clazz));
    }

    public static <T> ImmutableBean<T> forClass(Class<T> clazz) {
        return new ImmutableBean<>(clazz);
    }

    public ConstructorDescriptor lookupConstructor(JavaBeanDescriptor<?> descriptor) {
        ConstructorDescriptor constructor = null;

        Exceptions.thrownIfTrue(descriptor.getConstructors().isEmpty(), "No constructor found for " + descriptor);

        if (descriptor.isRecord()) {
            constructor = descriptor.getConstructors().iterator().next();
        } else {
            constructor = descriptor.getConstructors().iterator().next();
        }

        return constructor;
    }

}
