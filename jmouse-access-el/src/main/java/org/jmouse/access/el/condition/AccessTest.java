package org.jmouse.access.el.condition;

import org.jmouse.el.extension.Test;

import java.util.List;

/**
 * A test a policy condition may apply — <strong>and the only type {@link TestCatalog} will collect</strong>.
 *
 * <p>The mirror image of {@link AccessFunction}, for the shape a function cannot express well.
 *
 * <h2>A test, and not a function</h2>
 *
 * <p>{@code JMF-1} decided <em>"a function, not a test"</em> and was right — <strong>about
 * {@code consumed()}</strong>. A quota has a threshold, and a threshold belongs in the policy file where
 * a reader can see it: {@code consumed('ai-token', '3h') >= 100000} says what the limit is;
 * {@code is overQuota('ai-token')} hides it inside Java.
 *
 * <p>That was never a rule about the whole vocabulary. A genuinely boolean predicate has no threshold to
 * expose, and writing it as a function makes it worse prose, not better:
 *
 * <pre>
 * &#64;SPACE:'id-space-01' shipment:write deny when now is not workingHours      // reads
 * &#64;SPACE:'id-space-01' shipment:write deny when workingHours() == false           // does not
 * </pre>
 *
 * <p>So the two shapes coexist: a <strong>function</strong> answers <em>how much</em>, a <strong>test</strong>
 * answers <em>whether</em>.
 *
 * <h2>⚠️ Why collecting {@code Test} would have been a hole</h2>
 *
 * <p>{@code jmouse-el} ships its own tests — {@code NullTest}, {@code StartsTest}, {@code EndsTest} and
 * the rest — as ordinary {@link Test} implementations, and {@link ConditionDialect} chooses which of them
 * the restricted dialect gets by listing them. Asking a bean container for {@code List<Test>} would hand
 * it whatever any unrelated module happened to expose, the moment it exposed it: no rule changed, no test
 * failing.
 *
 * <p>A product implements <em>this</em> interface deliberately or contributes nothing. That is the
 * difference between a whitelist and a hope, and it is the same paragraph {@link AccessFunction} carries —
 * written twice on purpose, because the second one is the one somebody will shortcut.
 *
 * <h2>What a test may do</h2>
 *
 * <p>It may <strong>read</strong>, and it may not consume or write, for exactly the reasons on
 * {@link AccessFunction}: permission resolution is memoised per {@code (subject, scope chain)} and one
 * answer serves a whole page of rows.
 */
public interface AccessTest extends Test {

    /**
     * What a rule should read when this test <strong>cannot answer at all</strong> — the store it reads is
     * down, the schedule will not parse, the value it needs was never published.
     *
     * <p>{@code false} by default, meaning <em>fail closed</em>: {@code ConditionAxis} applies a deny whose
     * test could not answer and drops an allow whose test could not answer. Both refuse.
     *
     * <p>⚠️ Overriding this to {@code true} says a rule that cannot be evaluated should be <em>ignored</em>.
     * There are honest reasons for that, and every one of them should be written down next to the override.
     */
    default boolean failsOpen() {
        return false;
    }

    /**
     * Checked at load, once, against the literal arguments written in the policy file.
     *
     * <p>⚠️ An argument that is not a literal arrives as {@code null} — {@code is workingHours(someName)} —
     * and the honest answer is to decline to check it rather than to guess. A checker that guessed would
     * refuse rules that work, which is how a validator gets switched off.
     *
     * @param arguments the literal arguments, {@code null} for each one that could not be read
     */
    default void verifyArguments(List<String> arguments) {
    }
}
