package org.jmouse.script.el.budget;

import org.jmouse.el.evaluation.EvaluationContext;

/**
 * One dispatch's allowance, being spent.
 *
 * <h2>⚠️ One of these per dispatch, never per script</h2>
 *
 * <p>Two events are two budgets. A script is not a process and does not accumulate: a handler that used
 * its whole allowance on Tuesday starts Wednesday with all of it, because what a budget bounds is *one
 * thing the host asked for*, not a tenant's lifetime.</p>
 *
 * <h2>⚠️ An unlimited budget installs nothing</h2>
 *
 * <p>{@link #begin} answers {@code null} for a budget that stops nothing, and every caller is written to
 * take that as "no counting". So a host that declares no limit — a game, which writes its own scripts
 * and owns its own loop — pays one null check per body and nothing else at all. Charging every consumer
 * for one consumer's problem is how a guard becomes the reason nobody turns it on.</p>
 *
 * <h2>⚠️ Not thread-safe, and it must not be</h2>
 *
 * <p>It is a counter belonging to one evaluation on one thread. Making it safe to share would mean
 * atomics on the hottest path in the language, to support a thing nobody does — two threads dispatching
 * one event into one context is already a data race in everything else the context holds.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ScriptExecution {

    /**
     * How many steps between two readings of the clock.
     *
     * <p>⚠️ A deadline is the only limit that cannot be counted, and {@code System.nanoTime()} on every
     * statement would make the cheap limits pay for the expensive one. Checked every so often instead,
     * which overshoots a deadline by at most this many steps — a bound a host can reason about, unlike
     * "sometimes slower".</p>
     */
    private static final int CLOCK_EVERY = 256;

    private final String       document;
    private final ScriptBudget budget;
    private final long         expiresAt;

    private long steps;
    private int  depth;
    private int  sinceClockRead;

    private ScriptExecution(String document, ScriptBudget budget) {
        this.document = document;
        this.budget = budget;
        this.expiresAt = budget.deadline() == null
                ? Long.MAX_VALUE
                : System.nanoTime() + budget.deadline().toNanos();
    }

    /**
     * Starts spending a budget, or answers {@code null} when there is nothing to spend.
     *
     * @param document what the script is called, for the refusal
     * @param budget   the effective budget — already clamped, see {@link ScriptBudget#clampTo}
     * @return an execution, or {@code null} when the budget stops nothing
     */
    public static ScriptExecution begin(String document, ScriptBudget budget) {
        return budget == null || budget.isUnlimited() ? null : new ScriptExecution(document, budget);
    }

    /**
     * Returns the execution a context is carrying, if any.
     *
     * <p>⚠️ Read <strong>once per body</strong> and held in a local, never once per statement: this is a
     * map lookup, and the loop below it may run sixty times a second.</p>
     *
     * @param context the evaluation context
     * @return the execution, or {@code null} when nothing is being counted
     */
    public static ScriptExecution from(EvaluationContext context) {
        return context.getObject(ScriptExecution.class) instanceof ScriptExecution execution
                ? execution
                : null;
    }

    /**
     * Puts this execution where the nodes will find it.
     *
     * @param context the evaluation context this dispatch will run in
     */
    public void installInto(EvaluationContext context) {
        context.setObject(ScriptExecution.class, this);
    }

    /**
     * Takes this execution back out, so a context outlives one dispatch without outliving its budget.
     *
     * @param context the evaluation context
     */
    public static void clear(EvaluationContext context) {
        context.setObject(ScriptExecution.class, null);
    }

    /**
     * Spends one step, and checks the clock every so often.
     *
     * @param where what the script was doing, for the refusal — a constant, never a built string
     * @throws ScriptBudgetExceededException when there is nothing left
     */
    public void step(String where) {
        if (++steps > budget.steps()) {
            throw refuse(ScriptLimit.STEPS, budget.steps(), where);
        }

        if (++sinceClockRead >= CLOCK_EVERY && expiresAt != Long.MAX_VALUE) {
            sinceClockRead = 0;

            if (System.nanoTime() > expiresAt) {
                throw refuse(ScriptLimit.DEADLINE, budget.deadline().toMillis(), where);
            }
        }
    }

    /**
     * Records one turn of a loop.
     *
     * <p>⚠️ The count belongs to the loop rather than to the dispatch, so it is passed in. A shared
     * counter would let one runaway collection spend the whole allowance and leave the refusal pointing
     * at whatever ran next.</p>
     *
     * @param taken    how many turns this loop has taken, including this one
     * @param variable the loop's variable, for the refusal
     * @throws ScriptBudgetExceededException when this loop has gone round too many times
     */
    public void iteration(long taken, String variable) {
        if (taken > budget.loopIterations()) {
            throw refuse(ScriptLimit.LOOP_ITERATIONS, budget.loopIterations(),
                         "the loop over '" + variable + "'");
        }
    }

    /**
     * Records entry into a declared function.
     *
     * <p>⚠️ Every call must be paired with {@link #leave()} in a {@code finally}, or a function that
     * returned through a {@code return} would leave the depth raised forever and the next handler in the
     * same dispatch would be refused for something it did not do.</p>
     *
     * @param name the function's name, for the refusal
     * @throws ScriptBudgetExceededException when the call is too deep
     */
    public void enter(String name) {
        if (++depth > budget.recursionDepth()) {
            depth--;

            throw refuse(ScriptLimit.RECURSION_DEPTH, budget.recursionDepth(), "the call to '" + name + "'");
        }
    }

    /** Records the return from a declared function. */
    public void leave() {
        depth--;
    }

    private ScriptBudgetExceededException refuse(ScriptLimit limit, long allowed, String where) {
        return new ScriptBudgetExceededException(document, limit, allowed, where, budget.wasClamped(limit));
    }

    @Override
    public String toString() {
        return "EXECUTION['%s': %d steps of %s]".formatted(document, steps, budget);
    }
}
