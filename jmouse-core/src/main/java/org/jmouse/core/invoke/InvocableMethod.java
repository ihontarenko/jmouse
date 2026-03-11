package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.access.descriptor.MethodDescriptor;
import org.jmouse.core.access.descriptor.MethodIntrospector;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

/**
 * Bound method ready for invocation. 🎯
 */
public class InvocableMethod {

    private final Object                target;
    private final Method                method;
    private final MethodDescriptor      descriptor;
    private final List<MethodParameter> parameters;

    public InvocableMethod(Object target, Method method) {
        this.target = target;
        this.method = method;
        this.descriptor = new MethodIntrospector(method).introspect().toDescriptor();
        this.parameters = Stream.of(method.getParameters())
                .map(MethodParameter::forParameter)
                .toList();
    }

    public Object getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }

    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    public List<MethodParameter> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return Reflections.getMethodName(method);
    }
}