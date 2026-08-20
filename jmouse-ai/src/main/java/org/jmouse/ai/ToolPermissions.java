package org.jmouse.ai;

/**
 * How an action's permission is spelled when nobody spells it.
 *
 * <p><strong>One tool, one permission, and the action does not mention it.</strong>
 * {@code issues.create} costs {@code tool:issues_create}, derived from the name it already has, so a
 * definition declares its arguments and its handler and says nothing about authorization at all.
 *
 * <h2>⚠️ Why derived rather than declared</h2>
 *
 * <p>Because a name written twice is a name that can disagree with itself. Every product that wrote
 * these by hand ended up with the same three artefacts — a constant per action, a line per constant in
 * a policy document, and a startup check comparing the two — and the check existed only because the
 * hand-written half could drift. Worse, it could not catch the likeliest drift: a new action copied
 * from the one above it keeps that one's constant, both names exist, every set matches, and
 * {@code issues_create} quietly costs {@code tool:issues_delete}. Switching on one tool then switches on
 * another.
 *
 * <p>Derived, none of that is expressible. The permission is a function of the action's identity, there
 * is nothing to keep in step, and the only remaining question — <em>does this installation know that
 * name</em> — is one {@link ToolCatalog} already asks of the
 * {@link org.jmouse.ai.spi.PermissionVocabulary} and refuses to start without.
 *
 * <h2>⚠️ A derived permission is the tightest default, not a missing one</h2>
 *
 * <p>{@link ToolAction} used to insist the permission be given, on the reasoning that a default would
 * silently register an unguarded action. That reasoning is right about the defaults it had in mind — a
 * blank, or one permission shared by a whole tool — and backwards about this one. What is derived here
 * is <em>unique to the single action</em> and held by nobody until somebody grants it, so a definition
 * that forgets to think about authorization gets the narrowest possible answer rather than the widest.
 * An action can still declare something else, and a few must: one republished from a server this
 * application merely connects to is not this installation's to name.
 */
public final class ToolPermissions {

    /** What every derived name begins with, so a reader can tell the axis at a glance. */
    public static final String PREFIX = "tool:";

    private ToolPermissions() {
    }

    /** What reaching this action costs, unless it declared something else. */
    public static String of(ToolAction action) {
        return PREFIX + action.publishedName();
    }

    /**
     * The same, from the two halves of a name — for a builder, which has them before there is an action.
     *
     * <p>⚠️ It composes the published spelling rather than repeating the separator, so this and
     * {@link ToolAction#publishedName()} cannot come apart.
     */
    public static String of(String toolName, String actionName) {
        return PREFIX + ToolAction.publishedName(toolName, actionName);
    }
}
