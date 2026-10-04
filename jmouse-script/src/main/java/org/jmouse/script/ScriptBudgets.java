package org.jmouse.script;

import org.jmouse.script.el.budget.ScriptBudget;

import java.time.Duration;

/**
 * The ceilings a stage may choose between.
 *
 * <h2>⚠️ FOUR, BECAUSE THE MOMENTS COST DIFFERENT AMOUNTS</h2>
 *
 * <p>A product may have one ceiling for the whole installation and be right to, when every script runs
 * on a request thread inside a write: one number describes every case. Most products are not like that.
 * A rule asked once per item inside a walk, where a 750 ms deadline each would turn a two-minute run
 * into a day, is not the same kind of moment as a rule asked once after a network fetch that already
 * took a second.</p>
 *
 * <p>⚠️ <strong>A ceiling is not editable by the person who writes the rules</strong>, and that is
 * deliberate: whoever writes a runaway loop is whoever would raise the limit. A document may ask for
 * <em>less</em> — {@code ScriptBudget.clampTo} makes that the only direction available — and never for
 * more.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ScriptBudgets {

    /**
     * For a rule asked once per item inside a walk.
     *
     * <p>⚠️ The deadline is the figure that matters, and it is small on purpose. Four thousand items at
     * 750 ms of allowance each is over eight hours of permitted stalling in a job somebody expects to
     * take minutes. Fifty milliseconds is more than enough for a path test or a tag test, which is what
     * a rule at this moment honestly is.</p>
     */
    public static final ScriptBudget PER_ITEM = ScriptBudget.builder()
            .steps(2_000)
            .loopIterations(500)
            .recursionDepth(16)
            .deadline(Duration.ofMillis(50))
            .build();

    /**
     * For a rule that decides something a person is waiting on, or that runs inside a write.
     *
     * <p>Bounding latency is the point when somebody is watching a spinner.</p>
     */
    public static final ScriptBudget INTERACTIVE = ScriptBudget.builder()
            .steps(20_000)
            .loopIterations(5_000)
            .recursionDepth(32)
            .deadline(Duration.ofMillis(750))
            .build();

    /**
     * For a rule that runs once after work that already took seconds — an import, an enrichment, a
     * finished job.
     *
     * <p>⚠️ Still bounded. "It runs in the background" is not a reason to allow an infinite loop; it is a
     * reason to allow five seconds instead of one.</p>
     */
    public static final ScriptBudget BACKGROUND = ScriptBudget.builder()
            .steps(100_000)
            .loopIterations(20_000)
            .recursionDepth(32)
            .deadline(Duration.ofSeconds(5))
            .build();

    /**
     * For a rule that runs while somebody is opening a page.
     *
     * <h3>⚠️ THE TIGHTEST ONE, AND IT IS NOT A SMALLER {@link #INTERACTIVE}</h3>
     *
     * <p>{@code INTERACTIVE} bounds a moment a person <em>asked for</em> — a save, a decision they are
     * waiting on, where 750 ms is a spinner they understand. This bounds a moment a person did not ask
     * for and does not know about: they opened a page, and a rule somebody else wrote is now between
     * them and it.</p>
     *
     * <p>⚠️ And it fires on a scale nothing else does. A page is opened hundreds of times a day where a
     * walk runs once an hour, so the cost of being generous here is paid on every navigation by every
     * reader — including the ones who never wrote a rule.</p>
     *
     * <p>⚠️ A stage using this should also think hard about its LOG. A row per run at this frequency is
     * a table that outgrows the product's own data; logging here belongs switched off by default and
     * turned on for one rule, deliberately.</p>
     */
    public static final ScriptBudget PER_REQUEST = ScriptBudget.builder()
            .steps(1_000)
            .loopIterations(200)
            .recursionDepth(8)
            .deadline(Duration.ofMillis(25))
            .build();

    private ScriptBudgets() {
    }

}
