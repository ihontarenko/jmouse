package org.jmouse.core.metadata;

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
