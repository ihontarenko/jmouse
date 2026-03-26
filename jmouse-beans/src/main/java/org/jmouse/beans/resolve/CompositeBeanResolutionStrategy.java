package org.jmouse.beans.resolve;

import java.util.List;

public class CompositeBeanResolutionStrategy implements BeanResolutionStrategy {

    private final List<BeanResolver> resolvers;

    public CompositeBeanResolutionStrategy(List<BeanResolver> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public boolean supports(BeanResolutionRequest request) {
        return resolvers.stream().anyMatch(resolver -> resolver.supports(request));
    }

    @Override
    public Object resolve(BeanResolutionRequest request) {
        for (BeanResolver resolver : resolvers) {
            if (resolver.supports(request)) {
                Object resolved = resolver.resolve(request);
                if (resolved != null || !request.required()) {
                    return resolved;
                }
            }
        }

        if (request.required()) {
            throw new IllegalStateException(
                    "No bean resolver could resolve type '%s'".formatted(request.beanType())
            );
        }

        return null;
    }

}