package org.jmouse.security.authorization.method;


import org.jmouse.security.core.access.annotation.Authorize;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

public final class MethodAuthorizationRegistry {

    private final Map<Method, List<MethodAuthorizationAttribute>> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public List<MethodAuthorizationAttribute> getAttributes(Method method) {
        return cache.computeIfAbsent(method, this::resolveAttributes);
    }

    private List<MethodAuthorizationAttribute> resolveAttributes(Method method) {
        List<MethodAuthorizationAttribute> attributes = new ArrayList<>();

        attributes.addAll(resolveAttributesForElement(method));
        attributes.addAll(resolveAttributesForElement(method.getDeclaringClass()));

        return Collections.unmodifiableList(attributes);
    }

    private List<MethodAuthorizationAttribute> resolveAttributesForElement(AnnotatedElement annotatedElement) {
        List<MethodAuthorizationAttribute> attributes = new ArrayList<>();

        if (annotatedElement.isAnnotationPresent(Authorize.class)) {
            attributes.add(new MethodAuthorizationAttribute.Default(
                    MethodAuthorizationAttribute.Kind.PRE_AUTHORIZE,
                    annotatedElement.getAnnotation(Authorize.class).value(), // compile here ???
                    null));
        }

        return attributes;
    }
}
