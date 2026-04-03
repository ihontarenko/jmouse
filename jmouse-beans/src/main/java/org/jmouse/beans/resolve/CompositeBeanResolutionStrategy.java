package org.jmouse.beans.resolve;

import org.jmouse.core.Sorter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Composite {@link BeanResolutionStrategy} that delegates resolution to ordered resolvers. 🧩
 *
 * <p>Resolvers are sorted before execution, and the first successful result is returned.</p>
 */
public class CompositeBeanResolutionStrategy implements BeanResolutionStrategy {

    private final List<BeanResolver> resolvers;

    private final static Logger LOGGER = LoggerFactory.getLogger(CompositeBeanResolutionStrategy.class);

    /**
     * Creates strategy with registered resolvers.
     *
     * @param resolvers available resolvers
     */
    public CompositeBeanResolutionStrategy(List<BeanResolver> resolvers) {
        this.resolvers = resolvers;
    }

    /**
     * Checks whether at least one resolver supports the request.
     *
     * @param request resolution request
     * @return {@code true} if any resolver can handle it
     */
    @Override
    public boolean supports(BeanResolutionRequest request) {
        return resolvers.stream().anyMatch(resolver -> resolver.supports(request));
    }

    /**
     * Resolves bean using the first matching resolver in sorted order.
     *
     * @param request resolution request
     * @return resolved bean or {@code null} for optional unresolved dependency
     *
     * @throws IllegalStateException if no resolver could resolve a required dependency
     */
    @Override
    public Object resolve(BeanResolutionRequest request) {
        List<BeanResolver> resolvers = new ArrayList<>(this.resolvers);

        Sorter.sort(resolvers);

        for (BeanResolver resolver : resolvers) {
            if (resolver.supports(request)) {
                Object resolved = resolver.resolve(request);
                if (resolved != null || !request.required()) {
                    AnnotatedElement source = request.repository();
                    LOGGER.debug(
                            "✅ resolve [{}] 🧩 via '{}' → \uD83D\uDCCC type='{}' \uD83D\uDD0E source='{}'",
                            request.beanName() != null ? request.beanName() : "<by-type>",
                            resolver.getClass().getSimpleName(),
                            request.classType().getTypeName(),
                            source != null ? source.getClass().getSimpleName() : "n/a"
                    );
                    return resolved;
                }
            }
        }

        if (request.required()) {
            throw new IllegalStateException(
                    "No any bean resolver could resolve type '%s'".formatted(request.beanType())
            );
        }

        return null;
    }

}