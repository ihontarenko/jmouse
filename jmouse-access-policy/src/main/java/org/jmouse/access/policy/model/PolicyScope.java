package org.jmouse.access.policy.model;

/**
 * A scope exactly as written: a name, and an instance where one was written.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 *   {@literal @}INSTALLATION             -&gt;  new PolicyScope("INSTALLATION", null)
 *   {@literal @}SELF                     -&gt;  new PolicyScope("SELF",         null)
 *   {@literal @}SPACE                    -&gt;  new PolicyScope("SPACE",        null)      // role bodies only
 *   {@literal @}SPACE:kyiv               -&gt;  new PolicyScope("SPACE",        "kyiv")
 *   {@literal @}SPACE:${default.space}   -&gt;  new PolicyScope("SPACE",        "${default.space}")
 * </pre>
 *
 * <p>Both fields are text and neither is checked here. Whether {@code SPACE} is a scope this
 * installation registered is a question for binding, which has the catalogue; the parser's job is to
 * report faithfully what the file said. A {@code ${…}} is likewise left verbatim.
 *
 * <p>⚠️ <strong>The presence of {@link #instance} is the sharpest distinction in the language.</strong>
 * Inside a role, {@code SPACE space:write} means "this role carries the permission as far as a
 * workspace" — <em>which</em> workspace is decided by where the role is assigned. Written by a subject
 * without an instance, the same words would grant it in <strong>every</strong> workspace at once. That
 * is a privilege escalation, and it is the structural rule a parser can and must enforce: a role body
 * rejects an instance, and a subject body's place scopes must name one.
 *
 * @param kind     the scope name, as written
 * @param instance which one, as written, or {@code null} for the kind-only form
 */
public record PolicyScope(String kind, String instance) {

    /** A scope named without an instance: a universal scope, or a role's reach. */
    public static PolicyScope kind(String kind) {
        return new PolicyScope(kind, null);
    }

    public static PolicyScope of(String kind, String instance) {
        return new PolicyScope(kind, instance);
    }

    public boolean namesAnInstance() {
        return instance != null && !instance.isBlank();
    }

    @Override
    public String toString() {
        return namesAnInstance() ? "@" + kind + ":" + instance : "@" + kind;
    }
}
