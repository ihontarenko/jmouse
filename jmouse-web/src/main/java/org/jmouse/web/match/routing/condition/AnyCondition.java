package org.jmouse.web.match.routing.condition;

import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.routing.MappingMatcher;

/**
 * ✅ A {@link MappingMatcher} implementation that always matches any route.
 *
 * <p>Acts as a universal "match-all" condition — effectively bypassing
 * any selective filtering. Often used as a default or fallback matcher
 * when no specific conditions are required.</p>
 *
 * <p>✨ <b>Behavior:</b></p>
 * <ul>
 *   <li>{@link #matches(RequestRoute)} always returns {@code true}.</li>
 *   <li>{@link #compare(MappingMatcher, RequestRoute)} always returns {@code 0}.</li>
 * </ul>
 *
 * @see NoneCondition
 * @see MappingMatcher
 */
public class AnyCondition implements MappingMatcher {

    /**
     * ⚖️ Always returns {@code 0}, since this matcher has no priority rules.
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
     * ✅ Always returns {@code true}, indicating this matcher accepts all routes.
     *
     * @param item current request route
     * @return always {@code true}
     */
    @Override
    public boolean matches(RequestRoute item) {
        return true;
    }
}
