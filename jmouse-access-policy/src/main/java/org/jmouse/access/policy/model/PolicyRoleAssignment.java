package org.jmouse.access.policy.model;

/**
 * A subject holding a role, at a scope.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * subject u-42 {
 *     grants SPACE_ADMIN {@literal @}SPACE:kyiv
 *     grants VIEWER      {@literal @}SPACE:lviv
 * }
 * </pre>
 *
 * <p>produces
 *
 * <pre>
 *   new PolicyRoleAssignment("SPACE_ADMIN", new PolicyScope("SPACE", "kyiv"), new SourceSpan(2, 5))
 *   new PolicyRoleAssignment("VIEWER",      new PolicyScope("SPACE", "lviv"), new SourceSpan(3, 5))
 * </pre>
 *
 * <p>The scope is not decoration: a role assigned at {@code SPACE:kyiv} confers its bundle in Kyiv and
 * nowhere else, and the conferred reach of each bundled permission is the <em>narrower</em> of the
 * bundle's and this one's.
 *
 * @param roleName  as written; whether any file declares it is a question for binding
 * @param condition ⚠️ <strong>the condition source, verbatim, or null</strong> — {@code grants
 *                  ROLE_ALMOST_ADMIN @GLOBAL when caller.agent}. It narrows <em>this handing-out</em>
 *                  rather than the role, which is exactly where the design puts conditions: a role
 *                  says what a permission is worth, and who holds it under what circumstances is a
 *                  decision about one account. The condition distributes over every entry of the
 *                  bundle
 */
public record PolicyRoleAssignment(
        String roleName, PolicyScope scope, String condition, SourceSpan at) {

    /** The ordinary assignment: held whenever the subject is asked about. */
    public PolicyRoleAssignment(String roleName, PolicyScope scope, SourceSpan at) {
        this(roleName, scope, null, at);
    }

    /** Whether anything narrows this assignment. */
    public boolean isConditional() {
        return condition != null && !condition.isBlank();
    }
}
