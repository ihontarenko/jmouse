package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.Verify;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite {@link MethodArgumentResolver}. 🔗
 *
 * <p>
 * Maintains an ordered list of resolvers and delegates argument resolution
 * to the first resolver that supports the given {@link MethodParameter}.
 * </p>
 */
public class MethodArgumentResolverComposite implements MethodArgumentResolver {

    private final List<MethodArgumentResolver> resolvers = new ArrayList<>();

    /**
     * Adds resolver to the composite.
     *
     * @param resolver resolver to add
     *
     * @return this composite for chaining
     */
    public MethodArgumentResolverComposite addResolver(MethodArgumentResolver resolver) {
        resolvers.add(Verify.nonNull(resolver, "resolver"));
        return this;
    }

    /**
     * Returns {@code true} if any registered resolver supports the parameter.
     */
    @Override
    public boolean supports(MethodParameter parameter) {
        return resolvers.stream().anyMatch(resolver -> resolver.supports(parameter));
    }

    /**
     * Resolves argument using the first matching resolver.
     *
     * @throws IllegalStateException if no resolver supports the parameter
     */
    @Override
    public Object resolve(MethodParameter parameter, InvocationRequest request) {
        for (MethodArgumentResolver resolver : resolvers) {
            if (resolver.supports(parameter)) {
                return resolver.resolve(parameter, request);
            }
        }

        throw new IllegalStateException(
                "No argument resolver for parameter '%s' of method '%s'."
                        .formatted(parameter.getParameter().getName(), request.method())
        );
    }
}