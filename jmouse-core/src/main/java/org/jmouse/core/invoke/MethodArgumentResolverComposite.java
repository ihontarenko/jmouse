package org.jmouse.core.invoke;

import org.jmouse.core.MethodParameter;
import org.jmouse.core.Verify;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite resolver that delegates to the first matching resolver. 🔗
 */
public class MethodArgumentResolverComposite implements MethodArgumentResolver {

    private final List<MethodArgumentResolver> resolvers = new ArrayList<>();

    public MethodArgumentResolverComposite addResolver(MethodArgumentResolver resolver) {
        resolvers.add(Verify.nonNull(resolver, "resolver"));
        return this;
    }

    @Override
    public boolean supports(MethodParameter parameter) {
        return resolvers.stream().anyMatch(resolver -> resolver.supports(parameter));
    }

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