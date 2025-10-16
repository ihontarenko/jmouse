package org.jmouse.core.matcher;

import java.util.*;

/**
 * 🧩 Default immutable implementation of the {@link Match} interface.
 *
 * <p>Encapsulates the result of a matching operation, optionally carrying
 * multiple typed result values (called <em>facets</em>) keyed by their
 * class or interface.</p>
 *
 * <p>Instances are immutable and safely composable via {@link #mergeFrom(Match)},
 * with logical semantics aligned with {@link Match#and(Match...)} and
 * {@link Match#or(Match...)}.</p>
 *
 * <p>✨ <b>Highlights:</b></p>
 * <ul>
 *   <li>Immutable and thread-safe design.</li>
 *   <li>Facets stored in a typed map for flexible retrieval.</li>
 *   <li>Efficient shared {@link #MISS} singleton for unmatched results.</li>
 *   <li>Supports value-based equality and human-readable debugging.</li>
 * </ul>
 *
 * @see Match
 */
final class MatchResult implements Match {

    /**
     * 🚫 Shared singleton instance representing a failed match.
     */
    private static final MatchResult MISS = new MatchResult(false, Map.of());

    private final boolean               matched;
    private final Map<Class<?>, Object> bag;

    private MatchResult(boolean matched, Map<Class<?>, Object> bag) {
        this.matched = matched;
        this.bag = bag;
    }

    /**
     * 🚫 Returns the singleton representing an unmatched result.
     *
     * @return a shared {@link MatchResult} with {@code matched = false}
     */
    public static MatchResult miss() {
        return MISS;
    }

    /**
     * ✅ Creates a successful match with an empty facet bag.
     *
     * @return a new {@link MatchResult} with {@code matched = true}
     */
    public static MatchResult hit() {
        return new MatchResult(true, Map.of());
    }

    /**
     * 🧩 Creates a copy of another {@link Match}.
     *
     * <p>If the source is also a {@link MatchResult}, it is returned directly.
     * Otherwise, all present facets are copied into a new immutable instance.</p>
     *
     * @param other source match to copy
     * @return an immutable copy of the given match
     */
    public static MatchResult copyOf(Match other) {
        if (other instanceof MatchResult match) {
            return match;
        }

        if (other.missed()) {
            return miss();
        }

        Map<Class<?>, Object> bag = new LinkedHashMap<>();

        for (Class<?> type : other.types()) {
            other.get(type.asSubclass(Object.class))
                    .ifPresent(value -> bag.put(type, value));
        }

        return new MatchResult(true, Map.copyOf(bag));
    }

    /**
     * @return {@code true} if this result represents a successful match
     */
    @Override
    public boolean matched() {
        return matched;
    }

    /**
     * 🔍 Retrieves a facet by its type.
     *
     * <p>Performs exact match lookup first; if none found, returns
     * the first assignable value.</p>
     *
     * @param type the facet type to retrieve
     * @param <T>  the expected facet type
     * @return an {@link Optional} containing the value, or empty if none found
     */
    @Override
    public <T> Optional<T> get(Class<T> type) {
        if (missed()) {
            return Optional.empty();
        }

        Object exact = bag.get(type);

        if (exact != null) {
            return Optional.of(type.cast(exact));
        }

        for (Object value : bag.values()) {
            if (type.isInstance(value)) {
                return Optional.of(type.cast(value));
            }
        }

        return Optional.empty();
    }

    /**
     * @return a {@link Set} of all explicitly attached facet types
     */
    @Override
    public Set<Class<?>> types() {
        return bag.keySet();
    }

    /**
     * ➕ Attaches a value under its runtime type.
     *
     * <p>Returns this instance if unmatched or value is {@code null}.</p>
     *
     * @param value value to attach
     * @return a new {@link MatchResult} with the added facet
     */
    @Override
    @SuppressWarnings("unchecked")
    public MatchResult attach(Object value) {
        if (missed() || value == null) {
            return this;
        }

        Class<Object> type = (Class<Object>) value.getClass();

        return attach(type, value);
    }

    /**
     * ➕ Attaches a value under a specific type key.
     *
     * <p>Returns this instance if unmatched or value is {@code null}.</p>
     *
     * @param type  key type
     * @param value value to attach
     * @param <T>   value type
     * @return a new {@link MatchResult} with updated facets
     */
    @Override
    public <T> MatchResult attach(Class<T> type, T value) {
        if (missed() || value == null) {
            return this;
        }

        Map<Class<?>, Object> next = new LinkedHashMap<>(bag);
        next.put(type, value);

        return new MatchResult(true, Collections.unmodifiableMap(next));
    }

    /**
     * 🔗 Merges facets from another {@link Match} instance.
     *
     * <p>If either side is unmatched, this instance is returned unchanged.
     * On conflicts, values from the provided {@code other} take precedence.</p>
     *
     * @param other the match whose facets should be merged
     * @return a new {@link MatchResult} with merged facets
     */
    MatchResult mergeFrom(Match other) {
        if (this.missed() || other.missed()) {
            return this;
        }

        Map<Class<?>, Object> next = new LinkedHashMap<>(bag);

        for (Class<?> type : other.types()) {
            other.get(type.asSubclass(Object.class)).ifPresent(value -> next.put(type, value));
        }

        return new MatchResult(true, Collections.unmodifiableMap(next));
    }

    /**
     * @return a concise, human-readable representation of this match
     */
    @Override
    public String toString() {
        return missed() ? "Match[✗]" : "Match[✓ " + bag + "]";
    }

    /**
     * @return {@code true} if both instances have equal match state and facets
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof MatchResult match)) {
            return false;
        }

        return matched == match.matched && Objects.equals(bag, match.bag);
    }

    /**
     * @return hash code derived from match state and facet map
     */
    @Override
    public int hashCode() {
        return Objects.hash(matched, bag);
    }
}
