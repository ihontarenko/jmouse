package org.jmouse.core.descriptive;

public interface PropertyDescriptor {

    ClassDescriptor getType();

    MethodDescriptor getGetter();

    MethodDescriptor getSetter();

    default boolean isWritable() {
        return getSetter() != null;
    }

    default boolean isReadable() {
        return getGetter() != null;
    }

}
