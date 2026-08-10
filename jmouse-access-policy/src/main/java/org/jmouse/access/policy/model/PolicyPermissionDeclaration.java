package org.jmouse.access.policy.model;

/**
 * A permission the file declares, and what it is for.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * permissions {
 *     form:read     "Read forms"
 *     form:write    "Create and edit forms"
 * }
 * </pre>
 *
 * <p>produces
 *
 * <pre>
 *   new PolicyPermissionDeclaration("form:read",  "Read forms",             new SourceSpan(2, 5))
 *   new PolicyPermissionDeclaration("form:write", "Create and edit forms",  new SourceSpan(3, 5))
 * </pre>
 *
 * <p>The description is not decoration: a permission catalogue is what an administration screen lists
 * when somebody is choosing what to grant, and {@code form:write} on its own tells them nothing they
 * could not guess wrongly.
 *
 * @param description the human sentence, or null where the file gave none
 */
public record PolicyPermissionDeclaration(String name, String description, SourceSpan at) {
}
