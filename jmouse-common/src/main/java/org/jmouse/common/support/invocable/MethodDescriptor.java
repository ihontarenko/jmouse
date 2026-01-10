package org.jmouse.common.support.invocable;

import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;

public interface MethodDescriptor extends TypeDescriptor {

    ClassTypeDescriptor getClassTypeDescriptor();

    Method getNativeMethod();

    int getParametersCount();

    Class<?>[] getParameterTypes();

    default String getDetailedName() {
        return Reflections.getMethodName(getNativeMethod());
    }

}
