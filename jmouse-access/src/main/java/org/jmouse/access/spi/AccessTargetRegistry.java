package org.jmouse.access.spi;

import org.jmouse.access.AccessTarget;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    /**
     * {@code name → type} — the vocabulary a policy writes and {@code /access/kinds} publishes.
     *
     * <p>⚠️ <strong>Built from {@link AccessResourceName}, never from the class name.</strong> This is
     * the only index of resource names there is: a name exists because somebody wrote it on the type,
     * and a type nobody wrote one on cannot take part in access control at all.
     */
    private final Map<String, Class<?>>                  byName = new HashMap<>();

    public AccessTargetRegistry(List<AccessTargetResolver<?>> resolvers) {
        for (AccessTargetResolver<?> resolver : resolvers) {
            AccessTargetResolver<?> existing = byType.put(resolver.resourceType(), resolver);

            if (existing != null) {
                throw new IllegalStateException(
                        "Two resolvers answer for " + resolver.resourceType().getSimpleName() + ": "
                        + existing.getClass().getName() + " and " + resolver.getClass().getName()
                        + ". Where a resource lives is one fact with one answer.");
            }

            // Fails here rather than at a refusal: a resolver for an unnamed type is a type no rule can
            // ever name, and that has no runtime symptom of its own to discover it by.
            String   name  = resolver.resourceName();
            Class<?> taken = byName.put(name, resolver.resourceType());

            if (taken != null && taken != resolver.resourceType()) {
                throw new IllegalStateException(
                        "Two types are both called '" + name + "' in access control: " + taken.getName()
                        + " and " + resolver.resourceType().getName()
                        + ". A policy names a resource by that word, so it has to mean one type — give "
                        + "one of them a different @AccessResourceName.");
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

    /**
     * The type a policy means by this word, or empty where nothing claims it.
     *
     * <p>What {@code through form} resolves against, and what a {@code may} request's {@code kind} is
     * looked up in. Case-insensitive on the way in, so a policy may shout without meaning something else.
     */
    public Optional<Class<?>> typeNamed(String name) {
        return Optional.ofNullable(name).map(word -> byName.get(word.toLowerCase(Locale.ROOT)));
    }

    /**
     * The word this type is written as.
     *
     * <p>Asked of the <em>resolver</em> rather than of the annotation, so that a type whose name is
     * declared on its resolver — a library class that cannot carry the annotation — answers the same
     * word here as it does in the index.
     */
    public Optional<String> nameOf(Class<?> resourceType) {
        return Optional.ofNullable(byType.get(resourceType)).map(AccessTargetResolver::resourceName);
    }

    /**
     * Every resource name this build knows, sorted — the vocabulary itself.
     *
     * <p>⚠️ These are the words a rule may actually write, which is why a startup refusal prints this
     * and not a list of Java class names: a class name told a policy author nothing they could type.
     */
    public List<String> knownTypes() {
        return byName.keySet().stream().sorted().toList();
    }
}
