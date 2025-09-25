package org.jmouse.security.core.method;

import org.jmouse.security.core.SecurityEnvelope;
import org.jmouse.security.core.SecurityContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MethodEnvelope implements SecurityEnvelope {

    private final Map<String, Object> attributes = new HashMap<>();
    private final Object              target;
    private final Method              method;
    private final Object[]            arguments;
    private       SecurityContext     securityContext;

    public MethodEnvelope(Object target, Method method, Object[] arguments, SecurityContext context) {
        this.target = target;
        this.method = method;
        this.arguments = arguments != null ? arguments.clone() : new Object[0];
        this.securityContext = context;
    }

    public Object getTarget() {
        return target;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return arguments.clone();
    }

    @Override
    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    @Override
    public void setSecurityContext(SecurityContext context) {
        this.securityContext = context;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getNative(Class<T> type) {
        if (type.isInstance(method)) {
            return Optional.of((T) method);
        }

        if (type.isInstance(target)) {
            return Optional.of((T) target);
        }

        return Optional.empty();
    }

}
