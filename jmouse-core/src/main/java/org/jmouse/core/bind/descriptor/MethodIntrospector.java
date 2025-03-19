package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.internal.MethodData;
import org.jmouse.core.reflection.JavaType;

import java.lang.reflect.Method;

public class MethodIntrospector extends ExecutableIntrospector<MethodData, MethodIntrospector, Method, MethodDescriptor> {

    public MethodIntrospector(Method target) {
        super(target);
    }

    public MethodIntrospector returnType(ClassTypeDescriptor returnType) {
        container.setReturnType(returnType);
        return self();
    }

    public MethodIntrospector returnType() {
        JavaType returnType = JavaType.forMethodReturnType(container.getTarget());
        return returnType(new ClassTypeIntrospector(returnType).name().toDescriptor());
    }

    @Override
    public MethodIntrospector introspect() {
        return super.introspect().returnType();
    }

    @Override
    public MethodDescriptor toDescriptor() {
        return new MethodDescriptor(this, container);
    }

    @Override
    public MethodData getContainerFor(Method target) {
        return new MethodData(target);
    }

}
