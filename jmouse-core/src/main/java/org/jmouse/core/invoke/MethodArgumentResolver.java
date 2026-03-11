package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;

/**
 * Strategy for resolving a single method argument. 🧩
 *
 * <p>
 * Implementations determine whether they support a given
 * {@link MethodParameter} and provide the corresponding value
 * during method invocation.
 * </p>
 */
public interface MethodArgumentResolver {

    /**
     * Returns whether this resolver supports the given parameter.
     *
     * @param parameter method parameter
     *
     * @return {@code true} if this resolver can resolve the argument
     */
    boolean supports(MethodParameter parameter);

    /**
     * Resolves argument value for the given parameter.
     *
     * @param parameter method parameter
     * @param request   invocation request
     *
     * @return resolved argument value
     */
    Object resolve(MethodParameter parameter, InvocationRequest request);
}