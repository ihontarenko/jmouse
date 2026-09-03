package org.jmouse.script.el.budget;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

/**
 * How much one dispatch of a script may do.
 *
 * <h2>⚠️ Two of these, and the difference between them is the whole security argument</h2>
 *
 * <p>A host that stores scripts per tenant stores their budgets the same way — in a row beside the
 * source, edited on the same form as the code. That is the right product shape and it is also the shape
 * in which a budget becomes decorative: <strong>whoever writes the runaway loop is the person setting
 * the limit.</strong></p>
 *
 * <p>So a budget is either a <em>request</em> — what a document asked for — or a <em>ceiling</em>, which
 * a host sets somewhere its tenants cannot reach. {@link #clampTo} makes the one that actually runs, and
 * it is the minimum of the two, always. A request larger than the ceiling is <strong>clamped, never
 * refused</strong>: refusing would mean a workspace stops working because somebody changed a number on a
 * plan, which is a failure with no visible cause in a screen nobody was looking at.</p>
 *
 * <p>A request <em>smaller</em> than the ceiling is honoured as written, and that is a feature rather
 * than a tolerance — a handler on a hot path budgeted deliberately small fails in testing instead of
 * being slow in production.</p>
 *
 * <h2>⚠️ Unlimited is the default, and it is the right one</h2>
 *
 * <p>A game host writes its own scripts and owns its own loop; making it pay for a guard against itself
 * would be charging every consumer for one consumer's problem. {@link #unlimited()} costs nothing at
 * all — see {@link ScriptExecution#begin}.</p>
 *
 * @param steps          how much a dispatch may do — see {@link ScriptLimit#STEPS}
 * @param loopIterations how many times one {@code for} may go round
 * @param recursionDepth how deep a function may call a function
 * @param deadline       how long a dispatch may take, or {@code null} for no clock at all
 * @param clamped        which limits came from a ceiling rather than from the request — read by a
 *                       refusal so it can say that raising the number in the document will not help
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record ScriptBudget(
        long                 steps,
        long                 loopIterations,
        int                  recursionDepth,
        Duration             deadline,
        Set<ScriptLimit>     clamped
) {

    /** What an unset count means. Chosen so a comparison needs no branch for "no limit here". */
    public static final long NO_COUNT = Long.MAX_VALUE;

    /** What an unset depth means. */
    public static final int NO_DEPTH = Integer.MAX_VALUE;

    private static final ScriptBudget UNLIMITED =
            new ScriptBudget(NO_COUNT, NO_COUNT, NO_DEPTH, null, EnumSet.noneOf(ScriptLimit.class));

    public ScriptBudget {
        clamped = clamped == null || clamped.isEmpty()
                ? Set.of()
                : Set.copyOf(clamped);
    }

    /**
     * A budget that stops nothing.
     *
     * @return the unlimited budget
     */
    public static ScriptBudget unlimited() {
        return UNLIMITED;
    }

    /**
     * Starts describing a budget.
     *
     * @return a fresh builder, unlimited until something is set on it
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Whether this budget stops anything at all.
     *
     * <p>⚠️ Read by {@link ScriptExecution#begin} so an unlimited budget installs nothing into the
     * evaluation context and every check downstream disappears.</p>
     *
     * @return {@code true} when no limit is set
     */
    public boolean isUnlimited() {
        return steps == NO_COUNT && loopIterations == NO_COUNT && recursionDepth == NO_DEPTH
                && deadline == null;
    }

    /**
     * Whether a limit's value came from a ceiling rather than from what was asked for.
     *
     * @param limit the limit to ask about
     * @return {@code true} when a clamp decided this number
     */
    public boolean wasClamped(ScriptLimit limit) {
        return clamped.contains(limit);
    }

    /**
     * Returns the budget that actually runs: this one, held to a ceiling.
     *
     * <p>⚠️ <strong>The minimum of each limit, and the clamp is not optional.</strong> Whatever this
     * budget asks for, nothing here can exceed what the host allows — that is the only reason a
     * tenant-authored budget is safe to accept at all.</p>
     *
     * <p>⚠️ <strong>The requested numbers are not carried forward.</strong> A refusal only needs to say
     * that a ceiling decided the number, so an author does not spend the afternoon raising a figure that
     * does nothing; showing them "you asked for 200 000 and are running at 50 000" needs the request,
     * and whoever stored the request has it. A library holding a second copy is a second copy to keep
     * true.</p>
     *
     * @param ceiling the most any script here may ask for; {@code null} or unlimited leaves this budget
     *                exactly as it is
     * @return the effective budget
     */
    public ScriptBudget clampTo(ScriptBudget ceiling) {
        if (ceiling == null || ceiling.isUnlimited()) {
            return this;
        }

        Set<ScriptLimit> clampedHere = EnumSet.copyOf(clamped.isEmpty()
                                                              ? EnumSet.noneOf(ScriptLimit.class)
                                                              : EnumSet.copyOf(clamped));

        long     effectiveSteps = lower(steps, ceiling.steps, ScriptLimit.STEPS, clampedHere);
        long     effectiveLoop  = lower(loopIterations, ceiling.loopIterations,
                                        ScriptLimit.LOOP_ITERATIONS, clampedHere);
        int      effectiveDepth = (int) lower(recursionDepth, ceiling.recursionDepth,
                                              ScriptLimit.RECURSION_DEPTH, clampedHere);
        Duration effectiveTime  = shorter(deadline, ceiling.deadline, clampedHere);

        return new ScriptBudget(effectiveSteps, effectiveLoop, effectiveDepth, effectiveTime, clampedHere);
    }

    private static long lower(long requested, long ceiling, ScriptLimit limit, Set<ScriptLimit> clamped) {
        if (ceiling < requested) {
            clamped.add(limit);

            return ceiling;
        }

        return requested;
    }

    private static Duration shorter(Duration requested, Duration ceiling, Set<ScriptLimit> clamped) {
        if (ceiling == null) {
            return requested;
        }

        if (requested == null || ceiling.compareTo(requested) < 0) {
            clamped.add(ScriptLimit.DEADLINE);

            return ceiling;
        }

        return requested;
    }

    @Override
    public String toString() {
        if (isUnlimited()) {
            return "BUDGET[unlimited]";
        }

        return "BUDGET[steps=%s, loop=%s, depth=%s, deadline=%s%s]".formatted(
                steps == NO_COUNT ? "-" : steps,
                loopIterations == NO_COUNT ? "-" : loopIterations,
                recursionDepth == NO_DEPTH ? "-" : recursionDepth,
                deadline == null ? "-" : deadline,
                clamped.isEmpty() ? "" : ", clamped=" + clamped);
    }

    /**
     * Describes a budget one limit at a time. Anything left unset stops nothing.
     */
    public static final class Builder {

        private long     steps          = NO_COUNT;
        private long     loopIterations = NO_COUNT;
        private int      recursionDepth = NO_DEPTH;
        private Duration deadline;

        private Builder() {
        }

        /**
         * Sets how much a dispatch may do — statements, loop iterations and function calls together.
         *
         * @param steps the ceiling
         * @return this builder
         */
        public Builder steps(long steps) {
            this.steps = steps;
            return this;
        }

        /**
         * Sets how many times one {@code for} may go round.
         *
         * @param iterations the ceiling, per loop
         * @return this builder
         */
        public Builder loopIterations(long iterations) {
            this.loopIterations = iterations;
            return this;
        }

        /**
         * Sets how deep a function may call a function.
         *
         * @param depth the ceiling
         * @return this builder
         */
        public Builder recursionDepth(int depth) {
            this.recursionDepth = depth;
            return this;
        }

        /**
         * Sets how long a dispatch may take.
         *
         * <p>⚠️ Turning this on makes evaluation depend on a clock. A host replaying a simulation must
         * leave it off.</p>
         *
         * @param deadline how long, or {@code null} to leave the clock alone
         * @return this builder
         */
        public Builder deadline(Duration deadline) {
            this.deadline = deadline;
            return this;
        }

        /**
         * Builds the budget.
         *
         * @return an immutable budget
         */
        public ScriptBudget build() {
            return new ScriptBudget(steps, loopIterations, recursionDepth, deadline, Set.of());
        }
    }
}
