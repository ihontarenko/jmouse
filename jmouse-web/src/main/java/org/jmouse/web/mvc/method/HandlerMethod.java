package org.jmouse.web.mvc.method;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.bind.descriptor.MethodDescriptor;
import org.jmouse.core.bind.descriptor.MethodIntrospector;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

/**
 * 🎯 Represents a handler method bound to a specific bean instance.
 *
 * <p>This class encapsulates the target bean, its method,
 * method metadata (descriptor), and parameters introspection.
 *
 * <p>Example usage:
 * <pre>{@code
 * HandlerMethod handler = new HandlerMethod(context, controllerBean, method);
 * MethodDescriptor desc = handler.getDescriptor();
 * List<MethodParameter> params = handler.getParameters();
 * Object result = handler.getMethod().invoke(handler.getBean(), argValues...);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class HandlerMethod {

    private final Object                bean;
    private final Method                method;
    private final MethodDescriptor      descriptor;
    private final List<MethodParameter> parameters;

    /**
     * Constructs a HandlerMethod from given context, bean, and method.
     *
     * @param bean the bean instance containing the method
     * @param method the method to handle requests
     */
    public HandlerMethod(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
        this.descriptor = new MethodIntrospector(method).introspect().toDescriptor();
        this.parameters = Stream.of(method.getParameters())
                .map(MethodParameter::forParameter).toList();
    }

    /**
     * Returns the bean instance that owns the handler method.
     *
     * @return the target bean instance
     */
    public Object getBean() {
        return bean;
    }

    /**
     * Returns the handler method.
     *
     * @return the target method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Returns metadata descriptor for the handler method.
     *
     * @return method descriptor with introspection info
     */
    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * Returns a list of method parameters, introspected from the handler method.
     *
     * @return list of method parameters
     */
    public List<MethodParameter> getParameters() {
        return parameters;
    }

    /**
     * Returns a string representation with method name.
     *
     * @return method name string
     */
    @Override
    public String toString() {
        return Reflections.getMethodName(method);
    }
}
