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
 * <h2>{@code through} — where the permission is asked</h2>
 *
 * <pre>
 * permissions {
 *     field:write "Create and edit field definitions" through form
 * }
 * </pre>
 *
 * <p>Some rows have no place of their own. A field definition has no owner column and belongs to no
 * workspace, because one field stands on many forms — so a guard over a field could only ask about
 * whichever workspace the request happened to name, which meant anybody holding {@code field:write}
 * anywhere held it everywhere. {@code through} moves the question onto the rows that <em>do</em> have a
 * place: the forms the field stands on.
 *
 * <p>⚠️ <strong>It belongs on the permission, not on a grant.</strong> Where a permission is asked is a
 * property of the permission itself; every grant line in every installation is untouched by adding one.
 *
 * @param description the human sentence, or null where the file gave none
 * @param through     the resource this permission is asked about instead, or null for the ordinary case
 */
public record PolicyPermissionDeclaration(
        String name,
        String description,
        PolicyPermissionRedirect through,
        SourceSpan at
) {

    /** The three-argument shape, for anything with no {@code through} to state. */
    public PolicyPermissionDeclaration(String name, String description, SourceSpan at) {
        this(name, description, null, at);
    }
}
