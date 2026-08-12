package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;

import java.util.List;

/**
 * A role a subject holds at a scope, with the bundle it carries — one row of the grant store, in the
 * engine's words rather than in a product's.
 *
 * <p>It arrives with its provenance already attached, because the control room renders exactly what
 * the engine resolved, and a second query to explain a decision is a second query that can disagree
 * with it.
 *
 * <h2>⚠️ Where a condition on a role lives, and where it does not</h2>
 *
 * <p>An assignment may be conditional — <em>you hold this role when X</em> — and that condition sits
 * in the {@link #attribution}, because it is a fact about <strong>this handing-out</strong> rather
 * than about the role. A condition belonging to the role itself is on the {@link BundledPermission},
 * per entry. Both may be present and both apply; {@link GrantCondition#all} composes them where the
 * effective set is built.
 *
 * @param roleName    what the role is called, as a refusal and the control room name it
 * @param at          where it was handed out — the assignment's own scope, not the bundle's
 * @param bundle      the permissions it carries, each with its own reach and its own optional
 *                    narrowing
 * @param attribution who handed it out, where that rule is written, and what narrows the assignment
 */
public record RoleGrant(
        String                  roleName,
        ScopeReference          at,
        List<BundledPermission> bundle,
        GrantAttribution        attribution
) {

    public RoleGrant {
        bundle      = bundle == null ? List.of() : List.copyOf(bundle);
        attribution = attribution == null ? GrantAttribution.none() : attribution;
    }

    /** Whether anything narrows this assignment beyond the scope it applies at. */
    public boolean isConditional() {
        return attribution.isConditional();
    }
}
