package org.jmouse.access.policy.model;

import org.jmouse.access.VariableKind;

/**
 * A variable the file declares — a value that is true of every call, and where it comes from.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * variables {
 *     constant  deployment    "Which deployment this is"
 *     dynamic   ambientType   "What the workspace this call is in counts"
 * }
 * </pre>
 *
 * <p>produces
 *
 * <pre>
 *   new PolicyVariableDeclaration("deployment", VariableKind.CONSTANT,
 *                                 "Which deployment this is", new SourceSpan(2, 5))
 *   new PolicyVariableDeclaration("ambientType", VariableKind.DYNAMIC,
 *                                 "What the workspace this call is in counts", new SourceSpan(3, 5))
 * </pre>
 *
 * <h2>Why this is a block of its own and not an action's business</h2>
 *
 * <p>Because an action produces what its call is <em>about</em>, and a variable is simply there. Once
 * the two shared one list, every action had to name every variable, and a file said that
 * {@code entry.listByPurpose} produces {@code deployment} — which no route does and no reader would
 * believe. A statement made only to satisfy a check has stopped being a statement.
 *
 * <p>So this block says the thing once. A condition may read a variable under any action or under
 * none, and {@code produces} goes back to meaning what it says.
 *
 * <h2>⚠️ The kind is checked, in both directions</h2>
 *
 * <p>{@link VariableKind#CONSTANT} is what a publisher attaches outright and
 * {@link VariableKind#DYNAMIC} is what it defers, so the file and the code can be compared on more
 * than a name. That is worth having twice over: a rule's reader learns whether what they are reading
 * can differ between two calls, and a rule's cost is legible — a dynamic variable is work, done only
 * where a rule names it.
 *
 * @param description the human sentence, or null where the file gave none
 */
public record PolicyVariableDeclaration(
        String name, VariableKind kind, String description, SourceSpan at) {
}
