package org.jmouse.web.match.routing;

import org.jmouse.core.matcher.Match;
import org.jmouse.core.matcher.MatchOp;
import org.jmouse.core.matcher.Matcher;

/**
 * 🧭 Unified contract for route-level matchers that both evaluate and compare
 * HTTP routing conditions in the jMouse framework.
 *
 * <p>This interface extends both {@link Matcher} (boolean predicate view)
 * and {@link MatchOp} (structured result view), allowing matchers to produce
 * rich {@link Match} metadata while still supporting lightweight
 * boolean matching.</p>
 *
 * <p>✨ <b>Highlights:</b></p>
 * <ul>
 *   <li>Provides {@link #apply(Object)} for producing typed {@link Match} results.</li>
 *   <li>Supplies default {@link #matches(Object)} shortcut for simple checks.</li>
 *   <li>Can define priority ordering via {@link #compare(MappingMatcher, Object)}.</li>
 * </ul>
 *
 * <p>Common implementations include:</p>
 * <ul>
 *   <li>{@code HttpMethodMatcher}</li>
 *   <li>{@code RequestPathMatcher}</li>
 *   <li>{@code ConsumesMatcher}, {@code ProducesMatcher}</li>
 *   <li>{@code HttpHeaderMatcher}, {@code QueryParameterMatcher}</li>
 * </ul>
 *
 * @param <T> the input type (typically {@link org.jmouse.web.http.RequestRoute})
 *
 * @author Ivan Hontarenko
 */
public interface MappingMatcher<T> extends MatchOp<T, Match>, Matcher<T> {

    /**
     * 🎯 Performs the core evaluation logic, producing a {@link Match} result
     * that may contain detailed facets (e.g., path variables, parameters, etc.).
     *
     * @param input the candidate input to evaluate
     * @return a structured {@link Match} result describing the evaluation outcome
     */
    Match apply(T input);

    /**
     * ✅ Simplified boolean view of {@link #apply(Object)} — returns whether
     * the input successfully matched.
     *
     * @param input the candidate input
     * @return {@code true} if matched, otherwise {@code false}
     */
    @Override
    default boolean matches(T input) {
        return apply(input).matched();
    }

    /**
     * 🔄 {@link MatchOp} compatibility: simply delegates to {@link #apply(Object)}.
     *
     * @param input the candidate input
     * @return the produced {@link Match} result
     */
    @Override
    default Match match(T input) {
        return apply(input);
    }

    /**
     * ⚖️ Optional comparator hook allowing matchers to express ordering or
     * specificity preference during route resolution.
     *
     * <p>Example: a matcher with a more specific path may return a smaller value.</p>
     *
     * @param other   another matcher to compare against
     * @param context contextual input (e.g., current route)
     * @return negative if this matcher is more specific, zero if equal, positive if less specific
     */
    default int compare(MappingMatcher<?> other, T context) {
        return 0;
    }
}
