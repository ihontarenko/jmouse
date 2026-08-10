package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A role a subject holds at a scope, with the bundle it carries — one row of the grant store, in the
 * engine's words rather than in a product's.
 *
 * <p>It arrives with its provenance already attached ({@link #grantedBy}, {@link #since}) because the
 * control room renders exactly what the engine resolved, and a second query to explain a decision is a
 * second query that can disagree with it.
 *
 * @param roleName  what the role is called, as a refusal and the control room name it
 * @param at        where it was handed out — the assignment's own scope, not the bundle's
 * @param grantedBy who handed it out, where that was recorded
 * @param since     when, where that was recorded
 * @param bundle    the permissions it carries, each with its own reach
 * @param origin    whether a row or a line holds this — what decides which editor a screen may offer
 */
public record RoleGrant(
        String                  roleName,
        ScopeReference          at,
        String                  grantedBy,
        LocalDateTime           since,
        List<BundledPermission> bundle,
        GrantOrigin             origin
) {

    public RoleGrant {
        bundle = bundle == null ? List.of() : List.copyOf(bundle);
        origin = origin == null ? GrantOrigin.stored() : origin;
    }

    /**
     * A grant a table holds — the ordinary case, and the reason a store that has never heard of
     * {@link GrantOrigin} keeps compiling.
     */
    public RoleGrant(
            String                  roleName,
            ScopeReference          at,
            String                  grantedBy,
            LocalDateTime           since,
            List<BundledPermission> bundle) {

        this(roleName, at, grantedBy, since, bundle, GrantOrigin.stored());
    }
}
