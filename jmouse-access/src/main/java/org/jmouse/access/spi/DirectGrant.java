package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;

import java.time.LocalDateTime;

/**
 * One permission given to — or taken away from — a subject personally, at a scope.
 *
 * <p>The route around roles, for the cases roles are the wrong shape for: one person who needs one
 * extra thing, and one person who must not have something their role gives everybody.
 *
 * <p><strong>A denial is a grant with {@link #allowed} false, not an absence.</strong> That is the
 * whole reason this record carries {@link #reason} and {@link #grantedBy}: a permission somebody took
 * away is not the same fact as one nobody ever gave, and the difference is the only way anyone finds
 * out why a power vanished.
 *
 * @param permission what the grant is about
 * @param allowed    true to give, false to take away — and taking away wins, at every level
 * @param at         where it applies
 * @param grantedBy  who recorded it, where that was recorded
 * @param reason     why, where that was recorded
 * @param since      when, where that was recorded
 * @param origin     whether a row or a line holds this — what decides which editor a screen may offer
 * @param condition  ⚠️ <strong>carried, never evaluated here</strong>, or null for the ordinary
 *                   unconditional grant. Resolution stays row-independent and cacheable; the
 *                   condition is read afterwards by an axis that may only narrow. See
 *                   {@link GrantCondition}
 */
public record DirectGrant(
        String         permission,
        boolean        allowed,
        ScopeReference at,
        String         grantedBy,
        String         reason,
        LocalDateTime  since,
        GrantOrigin    origin,
        GrantCondition condition
) {

    public DirectGrant {
        origin = origin == null ? GrantOrigin.stored() : origin;
    }

    /** Whether anything narrows this grant beyond the scope it applies at. */
    public boolean isConditional() {
        return condition != null;
    }

    public DirectGrant(
            String         permission,
            boolean        allowed,
            ScopeReference at,
            String         grantedBy,
            String         reason,
            LocalDateTime  since,
            GrantOrigin    origin) {

        this(permission, allowed, at, grantedBy, reason, since, origin, null);
    }

    /**
     * An unconditional grant a table holds — the ordinary case, and the reason a store that has never
     * heard of {@link GrantOrigin} or {@link GrantCondition} keeps compiling.
     */
    public DirectGrant(
            String         permission,
            boolean        allowed,
            ScopeReference at,
            String         grantedBy,
            String         reason,
            LocalDateTime  since) {

        this(permission, allowed, at, grantedBy, reason, since, GrantOrigin.stored(), null);
    }
}
