package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;

/**
 * Resolves a single method argument. 🧩
 */
public interface MethodArgumentResolver {

    /**
     * Returns whether this resolver supports the given parameter.
     */
    boolean supports(MethodParameter parameter);

    /**
     * Resolves argument value for the given parameter.
     */
    Object resolve(MethodParameter parameter, InvocationRequest request);
}