package org.jmouse.web.match.routing.condition;

import org.jmouse.core.matcher.Match;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

/**
 * ✅ A {@link MappingMatcher} that always matches any route.
 *
 * <p>Acts as a universal "match-all" condition — useful as a default/fallback
 * where selective filtering is not required.</p>
 *
 * <p><b>Behavior:</b></p>
 * <ul>
 *   <li>{@link #apply(RequestRoute)} always returns {@link Match#hit()}.</li>
 *   <li>{@link #compare(MappingMatcher, RequestRoute)} always returns {@code 0}.</li>
 * </ul>
 *
 * @see NoneCondition
 */
public final class AnyCondition implements MappingMatcher<RequestRoute> {

    /**
     * Reusable singleton instance.
     */
    public static final AnyCondition INSTANCE = new AnyCondition();

    private AnyCondition() {
    }

    /**
     * Single source of truth: always a hit.
     */
    @Override
    public Match apply(RequestRoute route) {
        return Match.hit();
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
        return "AnyCondition[ALWAYS MATCH]";
    }
}
