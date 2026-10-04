package org.jmouse.script.stage;

/**
 * When a stage's rules run relative to the thing they are about — and therefore what they may do.
 *
 * <h2>⚠️ TWO GUARANTEES THAT CANNOT BE HAD AT ONCE</h2>
 *
 * <p>A rule that refuses something must run <em>before</em> it happens, which means on the caller's
 * thread, holding it up, able to break it. A rule that merely reacts must survive its own failure
 * without taking the thing it reacted to with it — which means running after the fact, where refusing is
 * meaningless because there is nothing left to refuse.</p>
 *
 * <p>Every attempt to serve both from one timing quietly gives each of them the weaker half.</p>
 *
 * <h2>⚠️ THE STAGE DECLARES THIS, NOT THE SCRIPT</h2>
 *
 * <p>Which means what a script may do at a place is knowable <b>before any script is written</b> — by
 * the screen that offers the place, and by the person choosing it. A stage cannot become able to refuse
 * because somebody wrote a refusing script; somebody has to declare it a decision, deliberately.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public enum StageTiming {

    /**
     * Runs before the consequence, on the caller's thread, and may refuse.
     *
     * <p>⚠️ A rule that throws here is contained and treated as a refusal with the thrower's message,
     * never allowed to unwind the caller. That is not politeness: a stage like {@code itemSeen} sits
     * inside a walk over thousands of items, and a rule that could end that walk would be a rule nobody
     * dares write.</p>
     */
    DECISION,

    /**
     * Runs after the fact, isolated.
     *
     * <p>⚠️ A rule that throws here is logged and dropped. The work already happened, and rolling it
     * back because somebody's rule had a typo would make the product less reliable than it was before
     * rules existed.</p>
     */
    OBSERVATION,

    /**
     * Runs before the consequence, on the caller's thread, and may CHANGE it.
     *
     * <p>⚠️ A rule does not reach into the product: it records amendments on the event, and the code
     * that raised the event applies them and writes each one into the trace under the rule's name. A
     * rule that throws here is contained like a decision's, and its amendments so far stay recorded.</p>
     */
    AMENDMENT

}
