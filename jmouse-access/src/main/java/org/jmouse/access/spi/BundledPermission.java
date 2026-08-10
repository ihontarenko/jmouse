package org.jmouse.access.spi;

import org.jmouse.access.ScopeKind;

/**
 * One permission inside a role, and how far the role carries it.
 *
 * <p>A role is not a flat list of permissions. {@code ROLE_USER} bundles {@code entry:write} over the
 * holder's own rows and {@code form:read} across the installation, and the difference between those
 * two is this field — without it a role would confer everything it contains as widely as it was
 * handed out, which is the shape {@code space:administer} used to have.
 *
 * @param permission what the entry allows
 * @param carriedAt  how far the role carries it, independent of where the role was handed out
 */
public record BundledPermission(String permission, ScopeKind carriedAt) {

    /**
     * Where this entry lands for an assignment made at a given scope: <strong>the narrower of the
     * two</strong>.
     *
     * <p>The rule the whole access-control cluster turns on, and it lives here — in the model — rather
     * than on whatever entity a product stores roles in. It is what keeps {@code space:write} inside
     * an installation-wide {@code ROLE_USER} from opening every workspace in the installation.
     *
     * <p>What the caller does with the answer is the other half of the rule, and just as sharp:
     * landing on a scope that <em>names an instance</em> confers <strong>nothing</strong>, because the
     * assignment never said which instance. Landing on one that names none — own rows — does confer,
     * because there is exactly one answer to "whose".
     */
    public ScopeKind conferredAt(ScopeKind assignedAt) {
        return carriedAt.isAtLeastAsWideAs(assignedAt) ? assignedAt : carriedAt;
    }
}
