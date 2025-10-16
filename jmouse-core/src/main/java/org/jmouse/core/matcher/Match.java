package org.jmouse.core.matcher;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 🎯 Represents a generic outcome of a matching step capable of carrying
 * multiple typed results — called <em>facets</em>.
 *
 * <p>A {@code Match} is more flexible than a simple boolean matcher:
 * it can attach and retrieve multiple result objects keyed by their
 * runtime or declared type, allowing composable and extensible
 * match pipelines.</p>
 *
 * <p>✨ <b>Highlights:</b></p>
 * <ul>
 *   <li>Supports composite operations ({@link #and(Match...)}, {@link #or(Match...)}).</li>
 *   <li>Allows typed attachment and retrieval of arbitrary values.</li>
 *   <li>Keeps "unmatched → unmatched" semantics to preserve logical flow.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Match match = Match.hit()
 *     .attach(User.class, user)
 *     .attach(Role.class, role);
 *
 * match.ifPresent(User.class, u -> log.info("Matched user: {}", u));
 * }</pre>
 */
public interface Match {

    /**
     * 🔗 Combines multiple matches using logical AND.
     *
     * <p>All provided matches must succeed; otherwise, the resulting
     * {@code Match} is a miss. Facets from all matches are merged into one,
     * with later matches overwriting conflicting keys.</p>
     *
     * @param matches the matches to combine
     * @return a new combined {@code Match}
     */
    static Match and(Match... matches) {
        for (Match match : matches) {
            if (match.missed()) {
                return Match.Result.miss();
            }
        }

        Match result = Match.Result.hit();

        for (Match match : matches) {
            result = result.merge(match);
        }

        return result;
    }

    /**
     * 🔀 Combines multiple matches using logical OR.
     *
     * <p>Returns the first successful match (by order).
     * If none succeed, returns {@link Match.Result#miss()}.</p>
     *
     * @param matches the matches to evaluate in order
     * @return the first successful match or a miss
     */
    static Match or(Match... matches) {
        Match   result = null;
        boolean finite = false;

        for (Match match : matches) {
            if (match.matched()) {
                result = Match.Result.copyOf(match);
                finite = true;
                break;
            }
        }

        if (!finite) {
            result = Match.Result.miss();
        }

        return result;
    }

    /**
     * 🚫 Returns a shared instance representing a failed match.
     *
     * @return a {@code Match} with no facets and {@code matched = false}
     */
    static Match miss() {
        return Match.Result.miss();
    }

    /**
     * ✅ Returns a new successful match with no facets attached.
     *
     * @return a {@code Match} with {@code matched = true}
     */
    static Match hit() {
        return Match.Result.hit();
    }

    /**
     * 🧩 Creates a match based on the boolean result.
     *
     * @param matched whether the match succeeded
     * @return {@link #hit()} if matched; otherwise {@link #miss()}
     */
    static Match of(boolean matched) {
        return matched ? hit() : miss();
    }

    /**
     * @return {@code true} if the condition matched successfully
     */
    boolean matched();

    /**
     * 🔗 Merges facets from another {@link Match} instance.
     *
     * <p>If either side is unmatched, this instance is returned unchanged.
     * On conflicts, values from the provided {@code other} take precedence.</p>
     *
     * @param other the match whose facets should be merged
     * @return a new {@link org.jmouse.core.matcher.Match.Result} with merged facets
     */
    Match merge(Match other);

    /**
     * @return {@code true} if the match failed
     */
    default boolean missed() {
        return !matched();
    }

    /**
     * 🎛 Retrieves an attached facet by its type.
     *
     * <p>If an exact type match exists, it is returned.
     * Otherwise, the first assignable instance is returned.</p>
     *
     * @param type the class of the facet to retrieve
     * @param <T>  the expected facet type
     * @return an {@link Optional} containing the facet if present
     */
    <T> Optional<T> get(Class<T> type);

    /**
     * @return all explicitly attached facet types
     */
    Set<Class<?>> types();

    /**
     * ➕ Attaches a value under its runtime class.
     *
     * <p>If this {@code Match} is unmatched, it remains unchanged.</p>
     *
     * @param value value to attach
     * @return this match (if unmatched) or a new one with the attached value
     */
    Match attach(Object value);

    /**
     * ➕ Attaches a value under the given key type.
     *
     * <p>If this {@code Match} is unmatched, it remains unchanged.</p>
     *
     * @param type  key class
     * @param value value to attach
     * @param <T>   value type
     * @return this match (if unmatched) or a new one with the attached facet
     */
    <T> Match attach(Class<T> type, T value);

    /**
     * 🔁 Transforms an existing facet to another type using the provided mapping function.
     *
     * <p>Only applies if the source facet is present and this match succeeded.</p>
     *
     * @param type source facet type
     * @param as   target facet type
     * @param fn   mapping function
     * @param <A>  source type
     * @param <B>  target type
     * @return updated {@code Match} with transformed facet
     */
    default <A, B> Match map(Class<A> type, Class<B> as, Function<? super A, ? extends B> fn) {
        return get(type).map(v -> attach(as, fn.apply(v))).orElse(this);
    }

    /**
     * ⚡ Executes an action if a facet of the specified type is present.
     *
     * @param type   facet type to check
     * @param action consumer to execute
     * @param <A>    facet type
     * @return this match (for fluent chaining)
     */
    default <A> Match ifPresent(Class<A> type, Consumer<? super A> action) {
        get(type).ifPresent(action);
        return this;
    }

    /**
     * 🧩 Default immutable implementation of the {@link Match} interface.
     *
     * <p>Encapsulates the result of a matching operation, optionally carrying
     * multiple typed result values (called <em>facets</em>) keyed by their
     * class or interface.</p>
     *
     * <p>Instances are immutable and safely composable via {@link #merge(Match)},
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
    final class Result implements Match {

        /**
         * 🚫 Shared singleton instance representing a failed match.
         */
        private static final Match.Result MISS = new Match.Result(false, Map.of());

        private final boolean               matched;
        private final Map<Class<?>, Object> bag;

        private Result(boolean matched, Map<Class<?>, Object> bag) {
            this.matched = matched;
            this.bag = bag;
        }

        /**
         * 🚫 Returns the singleton representing an unmatched result.
         *
         * @return a shared {@link org.jmouse.core.matcher.Match.Result} with {@code matched = false}
         */
        public static Match.Result miss() {
            return MISS;
        }

        /**
         * ✅ Creates a successful match with an empty facet bag.
         *
         * @return a new {@link org.jmouse.core.matcher.Match.Result} with {@code matched = true}
         */
        public static Match hit() {
            return new Match.Result(true, Map.of());
        }

        /**
         * 🧩 Creates a copy of another {@link Match}.
         *
         * <p>If the source is also a {@link org.jmouse.core.matcher.Match.Result}, it is returned directly.
         * Otherwise, all present facets are copied into a new immutable instance.</p>
         *
         * @param other source match to copy
         * @return an immutable copy of the given match
         */
        public static Match copyOf(Match other) {
            if (other instanceof Match match) {
                return match;
            }

            Map<Class<?>, Object> bag = new LinkedHashMap<>();

            for (Class<?> type : other.types()) {
                other.get(type.asSubclass(Object.class))
                        .ifPresent(value -> bag.put(type, value));
            }

            return new Match.Result(true, Map.copyOf(bag));
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
         * @return a new {@link org.jmouse.core.matcher.Match.Result} with the added facet
         */
        @Override
        @SuppressWarnings("unchecked")
        public Match attach(Object value) {
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
         * @return a new {@link org.jmouse.core.matcher.Match.Result} with updated facets
         */
        @Override
        public <T> Match.Result attach(Class<T> type, T value) {
            if (missed() || value == null) {
                return this;
            }

            Map<Class<?>, Object> next = new LinkedHashMap<>(bag);
            next.put(type, value);

            return new Match.Result(true, Collections.unmodifiableMap(next));
        }

        /**
         * 🔗 Merges facets from another {@link Match} instance.
         *
         * <p>If either side is unmatched, this instance is returned unchanged.
         * On conflicts, values from the provided {@code other} take precedence.</p>
         *
         * @param other the match whose facets should be merged
         * @return a new {@link org.jmouse.core.matcher.Match.Result} with merged facets
         */
        @Override
        public Match merge(Match other) {
            if (this.missed() || other.missed()) {
                return this;
            }

            Map<Class<?>, Object> next = new LinkedHashMap<>(bag);

            for (Class<?> type : other.types()) {
                other.get(type.asSubclass(Object.class)).ifPresent(value -> next.put(type, value));
            }

            return new Match.Result(true, Collections.unmodifiableMap(next));
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

            if (!(other instanceof Match.Result match)) {
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

}
