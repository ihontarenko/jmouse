package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;
import org.jmouse.access.Subject;

/**
 * Everything a condition may see — and nothing else is reachable from one.
 *
 * <p>Deliberately three things. A predicate that could reach a repository, a clock or a static would
 * be a predicate whose answer depends on something the rule does not mention, and an authorization
 * rule nobody can read from its own text is worse than no rule.
 */
public interface ConditionContext {

    /** Who is asking. Identifiers and flags only — a {@link Subject} carries nothing else. */
    Subject subject();

    /** The scope the grant is being evaluated at. */
    ScopeReference place();

    /**
     * The resolved target's row, read-only, or null where the call named none.
     *
     * <p>⚠️ A condition that dereferences it <strong>must tolerate null</strong>: plenty of routes are
     * aimed at a place rather than a row, and a rule that threw on those would refuse them for the
     * wrong reason.
     */
    Object resource();

    /** The three, as one value. */
    static ConditionContext of(Subject subject, ScopeReference place, Object resource) {
        return new Of(subject, place, resource);
    }

    record Of(Subject subject, ScopeReference place, Object resource) implements ConditionContext {
    }
}
