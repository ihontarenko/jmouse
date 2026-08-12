package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;

/**
 * One permission given to — or taken away from — a subject personally, at a scope.
 *
 * <p>The route around roles, for the cases roles are the wrong shape for: one person who needs one
 * extra thing, and one person who must not have something their role gives everybody.
 *
 * <p><strong>A denial is a grant with {@link #allowed} false, not an absence.</strong> That is the
 * whole reason an {@link #attribution} rides along: a permission somebody took away is not the same
 * fact as one nobody ever gave, and the difference is the only way anyone finds out why a power
 * vanished.
 *
 * @param permission  what the grant is about
 * @param allowed     true to give, false to take away — and taking away wins, at every level
 * @param at          where it applies
 * @param attribution who recorded it, where the rule is written, and what narrows it. ⚠️ The
 *                    condition inside it is <strong>carried, never evaluated</strong> during
 *                    resolution — see {@link GrantAttribution}
 */
public record DirectGrant(
        String           permission,
        boolean          allowed,
        ScopeReference   at,
        GrantAttribution attribution
) {

    public DirectGrant {
        attribution = attribution == null ? GrantAttribution.none() : attribution;
    }

    /** Whether anything narrows this grant beyond the scope it applies at. */
    public boolean isConditional() {
        return attribution.isConditional();
    }

    /** What narrows it, or null — read often enough by the resolver to be worth naming here. */
    public GrantCondition condition() {
        return attribution.condition();
    }
}
