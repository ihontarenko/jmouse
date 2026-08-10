package org.jmouse.access.policy.model;

/**
 * One permission inside a role, and how far the role carries it.
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
 * <p>produces, in order:
 *
 * <pre>
 *   new PolicyBundleEntry("space:write", "SPACE",        new SourceSpan(2, 20))
 *   new PolicyBundleEntry("form:read",   "INSTALLATION", new SourceSpan(3, 20))
 * </pre>
 *
 * <p>A role is not a flat list of permissions: {@code SPACE_ADMIN} may carry {@code space:write} as
 * far as a workspace and {@code form:read} across the installation, and the difference is this
 * entry's {@link #scope}.
 *
 * @param permission as written — {@code form:read}, or a namespace wildcard {@code form:*}, expanded
 *                   at binding rather than matched per request
 * @param scope      the scope <strong>name</strong> only. A bundle never names an instance: which one
 *                   is decided by where the role is assigned
 */
public record PolicyBundleEntry(String permission, String scope, SourceSpan at) {
}
