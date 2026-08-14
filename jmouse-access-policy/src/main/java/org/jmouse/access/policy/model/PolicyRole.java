package org.jmouse.access.policy.model;

import java.util.List;

/**
 * A named bundle of permissions, each with the reach the role carries it at.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * role SPACE_ADMIN {
 *     {@literal @}SPACE         space:write
 *     {@literal @}INSTALLATION  form:read
 * }
 * </pre>
 *
 * <p>produces
 *
 * <pre>
 *   new PolicyRole("SPACE_ADMIN", List.of(
 *           new PolicyBundleEntry("space:write", "SPACE",        new SourceSpan(2, 20)),
 *           new PolicyBundleEntry("form:read",   "INSTALLATION", new SourceSpan(3, 20))),
 *           new SourceSpan(1, 1))
 * </pre>
 *
 * <p>A role body holds only bundle entries — no nested roles, no conditions, and no denials.
 *
 * <p>⚠️ <strong>No {@code deny} inside a role is a deliberate limitation.</strong> Deny wins globally
 * and is applied last, so a denial in a bundle would take the permission away from everybody holding
 * the role <em>everywhere</em>, which is never what putting it there means. Denials are per-subject.
 *
 * <h2>{@code assignableAt} is a different question from a bundle entry's scope</h2>
 *
 * <p>A bundle entry says how far a permission reaches <em>once somebody holds the role</em>;
 * {@code assignableAt} says where the role may be <em>handed out</em>. The conferred reach is the
 * narrower of the two, so they are read together and neither can be worked out from the other:
 * {@code SPACE_ADMIN} bundles at {@code SPACE} and is assignable at {@code SPACE}, but a membership
 * marker bundling <em>nothing</em> is still assignable at exactly one place, and a role bundling at
 * {@code SELF} may well be handed out installation-wide.
 *
 * <p>⚠️ <strong>{@code null} means the document did not say</strong>, which is legitimate: a policy
 * that only ever feeds the engine never needs it. It becomes a requirement the moment a declared role
 * has to become a row, because {@code access_roles.assignable_at} is what stops a workspace role being
 * granted across an installation.
 *
 * @param name         what the role is called
 * @param assignableAt the widest scope kind it may be assigned at, as text; {@code null} where unstated
 * @param bundle       what it carries, and how far each entry reaches
 * @param at           where it was written
 */
public record PolicyRole(
        String                  name,
        String                  assignableAt,
        List<PolicyBundleEntry> bundle,
        SourceSpan              at
) {

    public PolicyRole {
        bundle = bundle == null ? List.of() : List.copyOf(bundle);
    }

    /** Whether the document stated where this role may be handed out. */
    public boolean statesWhereItMayBeAssigned() {
        return assignableAt != null && !assignableAt.isBlank();
    }
}
