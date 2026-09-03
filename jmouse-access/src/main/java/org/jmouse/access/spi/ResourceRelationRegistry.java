package org.jmouse.access.spi;

import org.jmouse.access.AccessTarget;

import java.util.HashMap;
import java.util.List;

import java.util.Map;
import java.util.Optional;

/**
 * Every relation a feature has declared, in one place the engine can walk.
 *
 * <p>The mirror of {@link AccessTargetRegistry}, and deliberately the same shape: it holds nothing of
 * its own, the features contribute, and a duplicate is a startup failure rather than a coin toss.
 *
 * <p>⚠️ <strong>Keyed on the PAIR, not on a name.</strong> A policy writes {@code through form}, and
 * {@code form} is a resource kind — so what has to be unique is "the relation from a Field to a Form",
 * not a bean called something. Two of those is ambiguity in an authorization rule, which is the one
 * place ambiguity may not be resolved by picking either.
 */
public class ResourceRelationRegistry {

    /** {@code (from, to) → relation}. */
    private final Map<Class<?>, Map<Class<?>, ResourceRelation<?>>> byPair = new HashMap<>();

    public ResourceRelationRegistry(List<ResourceRelation<?>> relations) {
        for (ResourceRelation<?> relation : relations) {
            ResourceRelation<?> existing = byPair
                    .computeIfAbsent(relation.from(), type -> new HashMap<>())
                    .put(relation.to(), relation);

            if (existing != null) {
                throw new IllegalStateException(
                        "Two relations lead from " + relation.from().getSimpleName()
                        + " to " + relation.to().getSimpleName() + ": "
                        + existing.getClass().getName() + " and " + relation.getClass().getName()
                        + ". A policy names the destination TYPE, so one pair is one answer — give the "
                        + "second path a name of its own before registering it.");
            }
        }
    }

    /**
     * The places a row borrows, or empty where nothing leads that way.
     *
     * <p>⚠️ The two empties are different and the caller must tell them apart: {@link Optional#empty()}
     * means <em>no such relation is declared</em> — a policy naming one is a configuration fault — while
     * a present-but-empty list means <em>this row hangs off nothing</em>, which is a refusal.
     */
    public Optional<List<AccessTarget>> targetsOf(Class<?> from, Class<?> to, String resourceId) {
        return relation(from, to).map(relation -> relation.targetsOf(resourceId));
    }

    /** Whether a relation exists between these two — what a startup check asks. */
    public boolean leadsFrom(Class<?> from, Class<?> to) {
        return relation(from, to).isPresent();
    }

    private Optional<ResourceRelation<?>> relation(Class<?> from, Class<?> to) {
        return Optional.ofNullable(byPair.get(from)).map(byDestination -> byDestination.get(to));
    }

    /**
     * Every relation this build declares, as {@code from → to} in the words a policy writes.
     *
     * <p>What a startup failure prints when a rule names a destination nothing leads to — a list of what
     * would have worked beats a message saying only that this did not.
     */
    public List<String> declared() {
        return byPair.entrySet().stream()
                .flatMap(from -> from.getValue().keySet().stream()
                        .map(to -> kindOf(from.getKey()) + " → " + kindOf(to)))
                .sorted()
                .toList();
    }

    /**
     * ⚠️ The word a policy writes for a type — {@link AccessResourceNames#of}, and deliberately nothing
     * else. This method existed as {@code getSimpleName().toLowerCase()} for exactly one afternoon and
     * was a second place that invented names; there is now one, and it is the annotation on the type.
     */
    public static String kindOf(Class<?> type) {
        return AccessResourceNames.of(type);
    }
}
