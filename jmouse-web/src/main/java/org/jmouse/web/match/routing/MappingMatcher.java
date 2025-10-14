package org.jmouse.web.match.routing;

import org.jmouse.core.matcher.Match;
import org.jmouse.core.matcher.MatchOp;

public interface MappingMatcher<I> extends MatchOp<I, Match> {

    /**
     * Core evaluation producing a {@link Match} with typed facets.
     */
    Match apply(I input);

    /**
     * Boolean view.
     */
    default boolean matches(I input) {
        return apply(input).matched();
    }

    /**
     * MatchOp compatibility: return the same {@link Match}.
     */
    @Override
    default Match match(I input) {
        return apply(input);
    }

    /**
     * Optional: prioritization against another matcher within context.
     */
    default int compare(MappingMatcher<?> other, I context) {
        return 0;
    }
}