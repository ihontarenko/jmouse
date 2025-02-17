package org.jmouse.core.bind.introspection.internal;

import org.jmouse.core.bind.introspection.ClassTypeDescriptor;

import java.lang.reflect.Method;

public class MethodData extends ExecutableData<Method> {

    private ClassTypeDescriptor returnType;

    public MethodData(Method target) {
        super(target);
    }

    public ClassTypeDescriptor getReturnType() {
        return returnType;
    }

    public void setReturnType(ClassTypeDescriptor returnType) {
        this.returnType = returnType;
    }

}
