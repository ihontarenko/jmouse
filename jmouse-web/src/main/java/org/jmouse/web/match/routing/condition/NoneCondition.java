package org.jmouse.web.match.routing.condition;

import org.jmouse.core.matcher.Match;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

/**
 * 🚫 A {@link MappingMatcher} that never matches any route.
 *
 * <p>Acts as a Null Object / placeholder to simplify pipelines where a non-null matcher
 * is required but no actual condition is defined.</p>
 *
 * <p><b>Behavior:</b></p>
 * <ul>
 *   <li>{@link #apply(RequestRoute)} always returns {@link Match#miss()}.</li>
 *   <li>{@link #matches(RequestRoute)} delegates to {@code apply(...).matched()} (always false).</li>
 *   <li>{@link #compare(MappingMatcher, RequestRoute)} always returns {@code 0}.</li>
 * </ul>
 */
public final class NoneCondition implements MappingMatcher<RequestRoute> {

    /**
     * Reusable singleton instance.
     */
    public static final NoneCondition INSTANCE = new NoneCondition();

    private NoneCondition() {
    }

    /**
     * Single source of truth: never matches.
     */
    @Override
    public Match apply(RequestRoute route) {
        return Match.miss();
    }

    /**
     * No ordering logic.
     */
    @Override
    public int compare(MappingMatcher<?> other, RequestRoute route) {
        return 0;
    }

    @Override
    public String toString() {
        return "NoneCondition[NEVER MATCH]";
    }
}
