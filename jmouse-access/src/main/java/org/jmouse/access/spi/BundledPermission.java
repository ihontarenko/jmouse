package org.jmouse.access.spi;

import org.jmouse.access.ScopeKind;

/**
 * One permission inside a role, how far the role carries it, and what — if anything — narrows it.
 *
 * <p>A role is not a flat list of permissions. {@code ROLE_USER} bundles {@code entry:write} over the
 * holder's own rows and {@code form:read} across the installation, and the difference between those
 * two is {@link #carriedAt} — without it a role would confer everything it contains as widely as it
 * was handed out, which is the shape {@code space:administer} used to have.
 *
 * <h2>⚠️ There is no attribution here, and that is the split the whole design rests on</h2>
 *
 * <p>Who handed a role out and when are facts about the <em>assignment</em> ({@link RoleGrant}), not
 * about the role. A bundle entry is part of the role's definition, so it carries only what the
 * definition can say: what, how far, and under what circumstances.
 *
 * @param permission what the entry allows
 * @param carriedAt  how far the role carries it, independent of where the role was handed out
 * @param condition  ⚠️ what narrows it, or null. <strong>Carried, never evaluated here.</strong>
 *                   Composed with the assignment's own condition when the effective set is built, and
 *                   read afterwards by an axis that may only narrow. There is deliberately no
 *                   {@code deny} in a bundle, so a {@code when} here can only ever <em>subtract from
 *                   an allow</em> — which is what makes a condition in a shared definition safe
 */
public record BundledPermission(String permission, ScopeKind carriedAt, GrantCondition condition) {

    /** The ordinary entry: nothing narrows it beyond the reach it is carried at. */
    public BundledPermission(String permission, ScopeKind carriedAt) {
        this(permission, carriedAt, null);
    }

    /**
     * Where this entry lands for an assignment made at a given scope: <strong>the narrower of the
     * two</strong>.
     *
     * <p>The rule the whole access-control cluster turns on, and it lives here — in the model —
     * rather than on whatever entity a product stores roles in. It is what keeps {@code space:write}
     * inside an installation-wide {@code ROLE_USER} from opening every workspace in the installation.
     *
     * <p>What the caller does with the answer is the other half of the rule, and just as sharp:
     * landing on a scope that <em>names an instance</em> confers <strong>nothing</strong>, because the
     * assignment never said which instance. Landing on one that names none — own rows — does confer,
     * because there is exactly one answer to "whose".
     */
    public ScopeKind conferredAt(ScopeKind assignedAt) {
        return carriedAt.isAtLeastAsWideAs(assignedAt) ? assignedAt : carriedAt;
    }

    /** Whether anything narrows this entry beyond the reach it is carried at. */
    public boolean isConditional() {
        return condition != null;
    }
}
