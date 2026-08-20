package org.jmouse.access.el.condition;

import org.jmouse.el.extension.Function;

import java.util.List;

/**
 * A function a policy condition may call — <strong>and the only type the catalogue will collect</strong>.
 *
 * <p>It adds one <em>optional</em> declaration to {@link Function} and requires nothing. It exists
 * mostly to be a name, and the name is the whole defence.
 *
 * <h2>⚠️ Why collecting {@code Function} would have been a hole</h2>
 *
 * <p>{@link ConditionDialect} closes three escape hatches by registering no function at all:
 * {@code class('fqcn')}, {@code set(…)} and reflection. All three ship in {@code jmouse-el} as ordinary
 * {@link Function} implementations — {@code ClassFunction}, {@code SetVariableFunction},
 * {@code GetVariableFunction}, {@code JavaReflectedFunction}. A catalogue built from
 * {@code List<Function>} out of a bean container would therefore hand every one of them back to the
 * restricted dialect the moment some <em>unrelated</em> module exposed the core extension as beans. No
 * policy would change, no test would fail, and a rule in an authorization file could call reflection.
 *
 * <p>A product implements this interface deliberately or contributes nothing. That is the difference
 * between a whitelist and a hope.
 *
 * <h2>What a condition function may do</h2>
 *
 * <p>It may <strong>read</strong>. Reading state is the entire reason this seam was opened — a rule
 * about how much somebody has used cannot be answered from the five things a
 * {@link org.jmouse.access.spi.ConditionContext} carries.
 *
 * <p>⚠️ It may <strong>not consume, and may not write</strong>. Conditions run on
 * {@code ConditionAxis}, whose permission resolution is memoised per {@code (subject, scope chain)} and
 * whose one answer serves a whole page of rows. A function that spent a quota would spend it as many
 * times as the cache missed, which is a number nobody can predict and nobody wants on an invoice.
 *
 * <p>⚠️ And it should be <strong>cheap</strong>. This runs on the decision path. A counter read is
 * fine; a join across three tables is a rule that makes every request slower whether or not anybody
 * wrote it.
 */
public interface AccessFunction extends Function {

    /**
     * What should happen to a rule whose call to this function could not be answered.
     *
     * <p>The default is <strong>false — fail closed</strong>: the rule is applied as though it held, so
     * a quota that cannot be read refuses rather than waves everybody through. A store being down is
     * the moment a limit matters most, and it is the moment a limit is least able to prove itself.
     *
     * <p>⚠️ Override to {@code true} only for a function whose rule is genuinely advisory — a signal
     * that improves a decision without being the reason for it. Anything protecting money, capacity or
     * data should leave this alone.
     *
     * <p>⚠️ It is declared <em>here</em>, on the function, rather than on each rule that calls it,
     * because whether a signal is advisory is a property of the signal. A rule-level override would let
     * the same function be safety-critical in one file and ignorable in another, which is a question
     * nobody reading either file could answer.
     *
     * @return whether an unanswerable call should be ignored instead of enforced
     */
    default boolean failsOpen() {
        return false;
    }

    /**
     * Checks, at load, that a call written in a policy is one this function could actually answer.
     *
     * <h2>⚠️ Why a wrong argument is worse than a wrong name</h2>
     *
     * <p>A misspelt function fails loudly — nothing registers it, and the load refuses. A misspelt
     * <em>argument</em> fails silently and permanently: {@code consumed('ai-token', 'week')} where
     * nothing records a {@code week} bucket reads zero, so the rule never holds and the limit it was
     * written to impose <strong>does not exist</strong>. No exception, no log line, no failing test.
     * The first symptom is a bill.
     *
     * <p>Only the function knows what its arguments mean, so only the function can say. Throwing from
     * here fails the load with the condition quoted beside the complaint, which is the last moment
     * anybody can still be told.
     *
     * <h2>⚠️ A null argument is "cannot be read", not "wrong"</h2>
     *
     * <p>The list carries one entry per argument in order, and an entry is {@code null} wherever the
     * argument was not a plain string literal — a variable, an expression, a number. That is a call
     * this check cannot reason about, <strong>not</strong> a call to reject: refusing it would refuse
     * rules that work, and a validator that refuses working rules is one somebody switches off. Skip
     * those and check the rest.
     *
     * <p>The default accepts everything, so a function with nothing to say about its arguments says
     * nothing.
     *
     * @param arguments the call's arguments as written, {@code null} where unreadable
     * @throws RuntimeException with a sentence naming what is wrong and what would have worked
     */
    default void verifyArguments(List<String> arguments) {
    }
}
