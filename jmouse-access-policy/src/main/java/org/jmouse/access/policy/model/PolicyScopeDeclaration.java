package org.jmouse.access.policy.model;

/**
 * A scope the file declares — one floor of the model, written down.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * scopes {
 *     INSTALLATION  everything
 *     ORGANIZATION  place  parameter=organizationId
 *     SPACE         place  parameter=spaceId
 *     SELF          own-rows
 * }
 * </pre>
 *
 * <p>produces, in order:
 *
 * <pre>
 *   new PolicyScopeDeclaration("INSTALLATION", "everything", null,               new SourceSpan(2, 5))
 *   new PolicyScopeDeclaration("ORGANIZATION", "place",      "organizationId",   new SourceSpan(3, 5))
 *   new PolicyScopeDeclaration("SPACE",        "place",      "spaceId",          new SourceSpan(4, 5))
 *   new PolicyScopeDeclaration("SELF",         "own-rows",   null,               new SourceSpan(5, 5))
 * </pre>
 *
 * <p>⚠️ <strong>Declaration order is the width order.</strong> There is no rank column and there must
 * not be one: a rank written beside each scope is a second statement of the same fact, and the day the
 * two disagree the covering chain silently reorders. The position in this block <em>is</em> the rank,
 * which is the same rule an enum's declaration order already follows.
 *
 * @param name      the scope name, as written
 * @param nature    {@code everything}, {@code place} or {@code own-rows}, as written. The parser does
 *                  not know what those mean; binding maps them onto
 *                  {@link org.jmouse.access.ScopeNature}
 * @param parameter the request parameter a route names an instance of this scope with, from
 *                  {@code parameter=…}, or null. Meaningful only for a place
 */
public record PolicyScopeDeclaration(String name, String nature, String parameter,
                                     String inside, String beside, String requires, SourceSpan at) {

    /**
     * ⚠️ {@code inside=} and {@code beside=} are two ways to write ONE fact, so writing both is refused
     * rather than reconciled: they are the same "second statement of the same fact" this block has
     * always been careful about.
     */
    public PolicyScopeDeclaration {
        if (inside != null && beside != null) {
            throw new IllegalArgumentException(
                "'" + name + "' declares both inside= and beside=, which are two ways of saying where "
                + "it sits. Say one: `beside=` is `inside=` whatever that one is inside.");
        }
    }

    /** The older four-argument form, for a scope that states no relation. */
    public PolicyScopeDeclaration(String name, String nature, String parameter, SourceSpan at) {
        this(name, nature, parameter, null, null, null, at);
    }
}
