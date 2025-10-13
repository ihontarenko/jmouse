package org.jmouse.web.match.routing.condition;

import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

/**
 * 🚫 A {@link MappingMatcher} implementation that never matches any route.
 *
 * <p>Acts as a "null-object" or placeholder matcher, useful for cases where
 * no real condition is defined but a non-null {@link MappingMatcher} instance
 * is still required (to simplify pipelines or comparisons).</p>
 *
 * <p>✨ <b>Behavior:</b></p>
 * <ul>
 *   <li>{@link #matches(RequestRoute)} always returns {@code false}.</li>
 *   <li>{@link #compare(MappingMatcher, RequestRoute)} always returns {@code 0}.</li>
 * </ul>
 *
 * @see MappingMatcher
 */
public class NoneCondition implements MappingMatcher {

    /**
     * ⚖️ Always returns {@code 0}, since this condition has no ordering logic.
     *
     * @param other        other matcher to compare with
     * @param requestRoute current route context
     * @return always {@code 0}
     */
    @Override
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        return 0;
    }

    /**
     * ❌ Always returns {@code false}, meaning this condition never matches.
     *
     * @param item current request route
     * @return always {@code false}
     */
    @Override
    public boolean matches(RequestRoute item) {
        return false;
    }
}
