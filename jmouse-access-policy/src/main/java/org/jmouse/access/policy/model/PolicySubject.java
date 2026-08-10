package org.jmouse.access.policy.model;

import java.util.List;

/**
 * What one account holds directly: the roles it was given, and the permissions written for it alone.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * subject u-42 {
 *     grants SPACE_ADMIN {@literal @}SPACE:kyiv
 *     {@literal @}SELF  form:write  deny
 * }
 * </pre>
 *
 * <p>produces
 *
 * <pre>
 *   new PolicySubject("u-42",
 *           List.of(new PolicyRoleAssignment("SPACE_ADMIN",
 *                       new PolicyScope("SPACE", "kyiv"), new SourceSpan(2, 5))),
 *           List.of(new PolicyGrant("form:write",
 *                       new PolicyScope("SELF", null), PolicyEffect.DENY, null, new SourceSpan(3, 5))),
 *           new SourceSpan(1, 1))
 * </pre>
 *
 * @param id as written, and possibly a {@code ${placeholder}} — resolved at binding, never here. A
 *           parser that resolved placeholders would need a property source, which is exactly the
 *           dependency this seam keeps out
 */
public record PolicySubject(
        String                     id,
        List<PolicyRoleAssignment> roles,
        List<PolicyGrant>          grants,
        SourceSpan                 at
) {

    public PolicySubject {
        roles  = roles  == null ? List.of() : List.copyOf(roles);
        grants = grants == null ? List.of() : List.copyOf(grants);
    }
}
