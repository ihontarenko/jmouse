package org.jmouse.access.policy;

import java.util.Set;

/**
 * The names one condition talks about — what makes a rule checkable before anybody runs it.
 *
 * <p>A condition is opaque to the engine on purpose: nothing looks inside a compiled predicate, and
 * an opaque callable answers the one question as well as a syntax tree would. That is right at
 * <em>decision</em> time and useless at <em>load</em> time, where the valuable question is a different
 * one — <strong>can this rule ever hold?</strong>
 *
 * <pre>
 * deny when action == 'label.print' and purpose != 'HOLDER'
 * </pre>
 *
 * <p>Both names exist. {@code label.print} is a real action; {@code purpose} is really published — by
 * a different route. So the rule never fires, the administrator believes printing is closed, and
 * nothing anywhere says otherwise. Name-checking passes this; only knowing that the rule mentions
 * <em>both</em> catches it.
 *
 * <p>⚠️ <strong>This is read off the source, so it is a best effort and says so.</strong> A condition
 * whose action comes out of a ternary, or which compares {@code action} to something other than a
 * literal, cannot be reduced to a set of names — and a checker that guessed would refuse rules that
 * are perfectly good. {@link #unknown()} is what that answers, and it checks nothing.
 *
 * @param actions the action names this condition compares {@code action} against. Empty where it
 *                names none, which means the rule holds in every call — almost never what anybody
 *                means, and the reason an unscoped rule can only be name-checked
 * @param values  every other bare name it reads, which is what it expects to have been published
 * @param certain whether the reading is exhaustive. False disables pair-checking for this rule rather
 *                than failing it: a rule nobody can analyse is not a rule that is wrong
 */
public record ConditionMentions(Set<String> actions, Set<String> values, boolean certain) {

    public ConditionMentions {
        actions = actions == null ? Set.of() : Set.copyOf(actions);
        values  = values  == null ? Set.of() : Set.copyOf(values);
    }

    /** What a compiler that cannot read its own source answers — and the default, so none must. */
    public static ConditionMentions unknown() {
        return new ConditionMentions(Set.of(), Set.of(), false);
    }

    /** Whether this rule can be checked against a catalogue at all. */
    public boolean isCheckable() {
        return certain;
    }

    /** Whether the rule bounds itself to particular actions, rather than holding in every call. */
    public boolean namesAnAction() {
        return !actions.isEmpty();
    }
}
