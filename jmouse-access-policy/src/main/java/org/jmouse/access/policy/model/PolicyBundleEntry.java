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
 * @param condition  ⚠️ <strong>the condition source, verbatim, or null.</strong> The parser does not
 *                   tokenise it, validate it, or know that an expression language exists.
 *                   <p>A role still carries no {@code deny}, which is what makes this safe: with no
 *                   way to write a denial in a bundle, a {@code when} here can only ever subtract
 *                   from an allow. ⚠️ It subtracts for <em>everybody holding the role</em>, though —
 *                   a condition that can never hold is a mass denial rather than one person's
 *                   missing permission, which is why an unresolvable name is refused at load
 */
public record PolicyBundleEntry(
        String permission, String scope, String condition, String reason, SourceSpan at) {

    /** The ordinary entry: nothing narrows it beyond the reach it is carried at. */
    public PolicyBundleEntry(String permission, String scope, SourceSpan at) {
        this(permission, scope, null, null, at);
    }

    /**
     * ⚠️ Kept so that adding {@code reason} left every existing construction site compiling, and so a
     * document without one is written back byte-for-byte as it was.
     */
    public PolicyBundleEntry(String permission, String scope, String condition, SourceSpan at) {
        this(permission, scope, condition, null, at);
    }

    /** Whether anything narrows this entry. */
    public boolean isConditional() {
        return condition != null && !condition.isBlank();
    }

    /** Whether this line says, in words, why it is what it is. */
    public boolean isExplained() {
        return reason != null && !reason.isBlank();
    }
}
