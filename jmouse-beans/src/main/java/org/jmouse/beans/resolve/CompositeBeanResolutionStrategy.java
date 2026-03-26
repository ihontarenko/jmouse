package org.jmouse.beans.resolve;

import java.util.List;

import static org.jmouse.core.Verify.nonNull;

/**
 * Composite {@link BeanResolutionStrategy} backed by ordered resolvers. 🔗
 *
 * <p>The first resolver that supports the context is used.</p>
 */
public class CompositeBeanResolutionStrategy implements BeanResolutionStrategy {

    private final List<BeanResolver> resolvers;

    public CompositeBeanResolutionStrategy(List<BeanResolver> resolvers) {
        this.resolvers = List.copyOf(nonNull(resolvers, "resolvers"));
    }

    @Override
    public boolean supports(BeanResolutionContext context) {
        return resolvers.stream().anyMatch(resolver -> resolver.supports(context));
    }

    @Override
    public Object resolve(BeanResolutionContext context) {
        for (BeanResolver resolver : resolvers) {
            if (resolver.supports(context)) {
                Object resolved = resolver.resolve(context);

                if (resolved != null || context.required()) {
                    return resolved;
                }
            }
        }

        if (context.required()) {
            throw new IllegalStateException(getErrorMessage(context));
        }

        return null;
    }

    protected String getErrorMessage(BeanResolutionContext context) {
        return "No bean resolver could resolve type '%s'"
                .formatted(context.type().getTypeName());
    }

}