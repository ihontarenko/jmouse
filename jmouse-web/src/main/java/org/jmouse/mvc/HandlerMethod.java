package org.jmouse.mvc;

import org.jmouse.core.bind.descriptor.MethodDescriptor;
import org.jmouse.core.bind.descriptor.MethodIntrospector;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.web.context.WebBeanContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

public class HandlerMethod {

    private final WebBeanContext        context;
    private final Object                bean;
    private final Method                method;
    private final MethodDescriptor      descriptor;
    private final List<MethodParameter> parameters;

    public HandlerMethod(WebBeanContext context, Object bean, Method method) {
        this.context = context;
        this.bean = bean;
        this.method = method;
        this.descriptor = new MethodIntrospector(method).introspect().toDescriptor();
        this.parameters = Stream.of(method.getParameters()).map(MethodParameter::ofParameter).toList();
    }

    public Object getBean() {
        return bean;
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
