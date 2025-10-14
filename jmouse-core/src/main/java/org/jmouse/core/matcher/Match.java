package org.jmouse.core.matcher;

import java.util.Optional;
import java.util.Set;
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
    @SafeVarargs
    static Match and(Match... matches) {
        for (Match match : matches) {
            if (match.missed()) {
                return MatchResult.miss();
            }
        }

        MatchResult result = MatchResult.hit();

        for (Match match : matches) {
            result = result.mergeFrom(match);
        }

        return result;
    }

    /**
     * 🔀 Combines multiple matches using logical OR.
     *
     * <p>Returns the first successful match (by order).
     * If none succeed, returns {@link MatchResult#miss()}.</p>
     *
     * @param matches the matches to evaluate in order
     * @return the first successful match or a miss
     */
    @SafeVarargs
    static Match or(Match... matches) {
        Match   result   = null;
        boolean finished = false;

        for (Match m : matches) {
            if (m.matched()) {
                result = MatchResult.copyOf(m);
                finished = true;
                break;
            }
        }

        if (!finished) {
            result = MatchResult.miss();
        }

        return result;
    }

    /**
     * 🚫 Returns a shared instance representing a failed match.
     *
     * @return a {@code Match} with no facets and {@code matched = false}
     */
    static Match miss() {
        return MatchResult.miss();
    }

    /**
     * ✅ Returns a new successful match with no facets attached.
     *
     * @return a {@code Match} with {@code matched = true}
     */
    static Match hit() {
        return MatchResult.hit();
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
}
