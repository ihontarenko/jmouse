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
 */
public record PolicyRole(String name, List<PolicyBundleEntry> bundle, SourceSpan at) {

    public PolicyRole {
        bundle = bundle == null ? List.of() : List.copyOf(bundle);
    }
}
