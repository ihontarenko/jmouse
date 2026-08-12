package org.jmouse.access.policy.model;

import java.util.List;

/**
 * An action the file declares, and the values it carries.
 *
 * <h2>Written as</h2>
 *
 * <pre>
 * actions {
 *     entry.list           "List submissions"
 *     entry.listByPurpose  "List submissions of one purpose"  publishes purpose, tier
 * }
 * </pre>
 *
 * <p>produces
 *
 * <pre>
 *   new PolicyActionDeclaration("entry.list", "List submissions", List.of(), new SourceSpan(2, 5))
 *   new PolicyActionDeclaration("entry.listByPurpose", "List submissions of one purpose",
 *                               List.of("purpose", "tier"), new SourceSpan(3, 5))
 * </pre>
 *
 * <h2>Why a file states this at all, when the code already does</h2>
 *
 * <p>For the reason it states its permissions: <strong>so a rule can be written without reading the
 * source.</strong> A condition mentioning {@code purpose} is legible only to somebody who knows which
 * calls publish one, and until it is written down here that knowledge lives in Java and in nobody's
 * head. The block is what an editor's completion offers and what a reviewer of the file checks a rule
 * against.
 *
 * <p>⚠️ <strong>Checked against the code, never read instead of it.</strong> The catalogue is derived
 * from the declarations on the routes; this block is a second statement of the same fact that fails
 * loudly the day the two disagree. That is the only mechanism that keeps documentation true rather
 * than merely well-intentioned — and it is why {@code publishes} lists names rather than describing
 * them: a name is checkable, prose is not.
 *
 * <p>⚠️ <strong>Dots, never colons.</strong> {@code entry:read} is a permission and {@code entry.read}
 * is an action, and they answer different questions: one is <em>may you</em>, the other is <em>what is
 * this</em>. Two vocabularies that looked alike would be misread in both directions on the first busy
 * day.
 *
 * @param description the human sentence, or null where the file gave none
 * @param values      the names this action publishes, in the order they were written. Empty where the
 *                    action carries nothing and is only a name a rule may scope itself to
 */
public record PolicyActionDeclaration(
        String name, String description, List<String> values, SourceSpan at) {

    public PolicyActionDeclaration {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
