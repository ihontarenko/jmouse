package org.jmouse.access.policy.model;

/**
 * One permission given to — or taken away from — one subject, at one scope.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * subject u-42 {
 *     {@literal @}SPACE:kyiv  entry:read
 *     {@literal @}SELF        form:write  deny
 *     {@literal @}SPACE:kyiv  entry:write allow  if resource.status == 'DRAFT'
 * }
 * </pre>
 *
 * <p>produces, in order:
 *
 * <pre>
 *   new PolicyGrant("entry:read",  new PolicyScope("SPACE", "kyiv"), ALLOW, null, new SourceSpan(2, 5))
 *   new PolicyGrant("form:write",  new PolicyScope("SELF",  null),   DENY,  null, new SourceSpan(3, 5))
 *   new PolicyGrant("entry:write", new PolicyScope("SPACE", "kyiv"), ALLOW,
 *                   "resource.status == 'DRAFT'", new SourceSpan(4, 5))
 * </pre>
 *
 * @param permission as written, possibly a namespace wildcard such as {@code form:*}
 * @param scope      where it applies
 * @param effect     {@link PolicyEffect#ALLOW} where the file wrote nothing
 * @param condition  ⚠️ <strong>the condition source, verbatim, or null.</strong> The parser does not
 *                   tokenise it, validate it, or know that an expression language exists
 */
public record PolicyGrant(
        String       permission,
        PolicyScope  scope,
        PolicyEffect effect,
        String       condition,
        String       reason,
        SourceSpan   at
) {

    /**
     * The four-part form, for everything that has no sentence to say.
     *
     * <p>⚠️ Kept so that adding {@code reason} did not touch twenty-three construction sites, and so a
     * document without one is <strong>byte-for-byte</strong> what it was — which matters more than it
     * looks: {@code PolicySeedStep}'s checksum is taken from {@code PolicyWriter}'s output, so a writer
     * that respelled unchanged documents would fail every installation's bootstrap ledger once.
     */
    public PolicyGrant(
            String permission, PolicyScope scope, PolicyEffect effect, String condition, SourceSpan at) {

        this(permission, scope, effect, condition, null, at);
    }

    /** Whether this grant says, in words, why it is what it is. */
    public boolean isExplained() {
        return reason != null && !reason.isBlank();
    }

    /**
     * Whether this grant is conditional.
     *
     * <p>⚠️ A document containing conditions <strong>fails to bind</strong> while nothing evaluates
     * one. Not "ignore the condition": a grant reading {@code allow if X} that grants unconditionally
     * is a hole with a comment explaining itself — and a conditional <em>deny</em> silently dropped is
     * the same failure wearing the opposite sign.
     */
    public boolean isConditional() {
        return condition != null && !condition.isBlank();
    }

    /**
     * The condition is text rather than a parsed tree on purpose.
     *
     * <p>Modelling it as {@code Condition(Property(…), Literal(…), EQ)} means owning operator
     * precedence, associativity and coercion — a second implementation of a grammar that already
     * exists, whose disagreements with the first are security incidents. It also cannot round-trip:
     * the control room has to show an administrator what they wrote, and rendering a tree back gives
     * <em>a</em> rendering, not <em>the</em> line. And nothing ever looks inside a condition; the
     * engine asks one question, which an opaque callable answers as well as a tree does.
     */
    public static PolicyGrant allow(String permission, PolicyScope scope, SourceSpan at) {
        return new PolicyGrant(permission, scope, PolicyEffect.ALLOW, null, at);
    }

    public static PolicyGrant deny(String permission, PolicyScope scope, SourceSpan at) {
        return new PolicyGrant(permission, scope, PolicyEffect.DENY, null, at);
    }
}
