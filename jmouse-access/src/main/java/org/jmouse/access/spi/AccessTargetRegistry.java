package org.jmouse.access.spi;

import org.jmouse.access.AccessTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every feature's answer to "where do my rows live", in one place the engine can ask.
 *
 * <p>It holds nothing of its own: the resolvers are contributed by the features, collected here, and
 * looked up by resource type. A duplicate is a startup failure rather than a coin toss — two answers
 * to one question is how an ownership check comes to disagree with a listing predicate, which is the
 * defect this whole seam exists to end.
 */
public class AccessTargetRegistry {

    private final Map<Class<?>, AccessTargetResolver<?>> byType = new HashMap<>();

    public AccessTargetRegistry(List<AccessTargetResolver<?>> resolvers) {
        for (AccessTargetResolver<?> resolver : resolvers) {
            AccessTargetResolver<?> existing = byType.put(resolver.resourceType(), resolver);

            if (existing != null) {
                throw new IllegalStateException(
                        "Two resolvers answer for " + resolver.resourceType().getSimpleName() + ": "
                        + existing.getClass().getName() + " and " + resolver.getClass().getName()
                        + ". Where a resource lives is one fact with one answer.");
            }
        }
    }

    /**
     * Where one row lives, or nothing — because there is no such row, or because nothing speaks for
     * its type.
     *
     * <p>The two are told apart by {@link #speaksFor}: a type nobody resolves is a gap in the wiring
     * and is reported as one by {@code @RequiresAccess} at startup, whereas an unknown identifier is
     * an ordinary answer the engine turns into a 404.
     */
    public Optional<AccessTarget> resolve(Class<?> resourceType, String resourceId) {
        AccessTargetResolver<?> resolver = byType.get(resourceType);

        return resolver == null ? Optional.empty() : resolver.resolve(resourceId);
    }

    /** A page of rows, through whatever batching the feature's resolver offers. */
    public Map<String, AccessTarget> resolveAll(Class<?> resourceType, List<String> resourceIds) {
        AccessTargetResolver<?> resolver = byType.get(resourceType);

        return resolver == null ? Map.of() : resolver.resolveAll(resourceIds);
    }

    /** Whether any feature has claimed this type. What a missing annotation target is checked against. */
    public boolean speaksFor(Class<?> resourceType) {
        return byType.containsKey(resourceType);
    }

    /** Every type somebody speaks for — the list a startup check prints when one is missing. */
    public List<String> knownTypes() {
        return byType.keySet().stream().map(Class::getSimpleName).sorted().toList();
    }
}
