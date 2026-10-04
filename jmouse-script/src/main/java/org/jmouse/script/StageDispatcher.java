package org.jmouse.script;

import org.jmouse.script.el.budget.ScriptBudgetExceededException;
import org.jmouse.script.event.AmendableEvent;
import org.jmouse.script.event.DecisionEvent;
import org.jmouse.script.event.DomainEvent;
import org.jmouse.script.spi.OwnTransaction;
import org.jmouse.script.spi.ScriptStage;
import org.jmouse.script.stage.StageRegistry;
import org.jmouse.script.stage.StageTiming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The three verbs — and the one place that decides what a failing rule costs.
 *
 * <h2>⚠️ THREE METHODS, NOT ONE WITH A FLAG</h2>
 *
 * <ul>
 *   <li>{@link #decide} — before the consequence, on the caller's thread; a rule may refuse;</li>
 *   <li>{@link #amend} — before the consequence; a rule may change the outcome;</li>
 *   <li>{@link #observe} — after the fact, in its own transaction; a rule can affect nothing.</li>
 * </ul>
 *
 * <p>Each one treats a thrown rule differently, and that is the reason they are separate methods: a
 * refusal at {@code decide} is an answer, the same refusal at {@code observe} is somebody believing they
 * prevented something that already happened, and a budget overrun is neither.</p>
 *
 * <h2>⚠️ NOTHING HERE LISTENS — THE PRODUCT WIRES THAT</h2>
 *
 * <p>Where this came from, these three were annotated event listeners, and {@code observe} in
 * particular was bound to the phase after a commit. Both are a container's business: this library knows
 * what to do when a moment arrives, not how a product announces one. A product publishes its events
 * however it likes and calls the matching verb.</p>
 *
 * <p>⚠️ One thing a product must keep when it wires {@link #observe}: run it <b>after</b> the work it
 * observes has committed. Running it inside the same transaction would let an observation rule's failure
 * roll back the thing it was only supposed to watch.</p>
 *
 * <h2>⚠️ IT LIVES BESIDE {@link AmendmentScope}, AND THAT IS NOT TIDINESS</h2>
 *
 * <p>{@code AmendmentScope.enter} and {@code leave} are package-private on purpose: they set a thread's
 * state that must be cleared in a {@code finally}, and a third caller that forgot one would hand the
 * next request somebody else's event. Keeping the only two callers in this package is what enforces
 * that — making them public to move this class elsewhere would have removed the guard to keep the
 * layout.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class StageDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageDispatcher.class);

    private final StageRegistry  registry;
    private final ScriptRuntime  runtime;
    private final OwnTransaction ownTransaction;

    public StageDispatcher(StageRegistry registry, ScriptRuntime runtime, OwnTransaction ownTransaction) {
        this.registry = registry;
        this.runtime = runtime;
        this.ownTransaction = ownTransaction == null ? OwnTransaction.none() : ownTransaction;
    }

    /**
     * Runs the rules that may refuse this, before it happens.
     *
     * <p>⚠️ A rule that throws is contained. It never unwinds the caller — a decision stage sits inside
     * walks over thousands of items, and a rule that could end one would be a rule nobody dares
     * write.</p>
     *
     * @param event what is about to happen
     */
    public void decide(DecisionEvent event) {
        ScriptStage<?> stage = stageAt(event, StageTiming.DECISION);

        if (stage == null) {
            return;
        }

        try {
            runtime.dispatch(stage, event.scope(), valuesOf(stage, event));
        } catch (RuntimeException thrown) {
            ScriptRefusedException objection = causeOf(thrown, ScriptRefusedException.class);

            if (objection != null) {
                // ⚠️ The rule worked. Its message is a phrase somebody will read on a screen, so it is
                // passed through untouched rather than wrapped in wording of ours.
                event.refuse(stage.name(), objection.getMessage());

                return;
            }

            // ⚠️ A rule that ran out of budget has NOT refused, and treating it as a refusal would be
            // the worst reading available: a loop with a typo would quietly start rejecting everything,
            // and every item would carry a plausible-looking reason.
            if (causeOf(thrown, ScriptBudgetExceededException.class) != null) {
                LOGGER.warn("rule at '{}' exceeded its budget and was abandoned", stage.name(), thrown);

                return;
            }

            LOGGER.warn("rule at '{}' failed and was ignored", stage.name(), thrown);
        }
    }

    /**
     * Runs the rules that may change this, before it happens.
     *
     * <p>⚠️ A rule that fails halfway leaves the amendments it already made standing. They were each
     * recorded with a reason and attributed, so half of a rule's intent is visible and explained rather
     * than silently discarded.</p>
     *
     * @param event what is about to happen, and what may be changed about it
     */
    public void amend(AmendableEvent event) {
        ScriptStage<?> stage = stageAt(event, StageTiming.AMENDMENT);

        if (stage == null) {
            return;
        }

        AmendmentScope.enter(event);

        try {
            if (event.onlyRule() != null) {
                runtime.dispatchOne(stage, event.scope(), event.onlyRule(), valuesOf(stage, event));
            } else {
                runtime.dispatch(stage, event.scope(), valuesOf(stage, event));
            }
        } catch (RuntimeException thrown) {
            if (causeOf(thrown, ScriptBudgetExceededException.class) != null) {
                LOGGER.warn("rule at '{}' exceeded its budget and was abandoned", stage.name(), thrown);

                return;
            }

            LOGGER.warn("rule at '{}' failed; the amendments it made before failing stand", stage.name(), thrown);
        } finally {
            AmendmentScope.leave();
        }
    }

    /**
     * Runs the rules that only watch this, after it has happened.
     *
     * <p>⚠️ IN A TRANSACTION OF ITS OWN — see {@link OwnTransaction}, which exists because of what
     * happens when it is not.</p>
     *
     * @param event what happened
     */
    public void observe(DomainEvent event) {
        ScriptStage<?> stage = stageAt(event, StageTiming.OBSERVATION);

        if (stage == null) {
            return;
        }

        try {
            ownTransaction.run(() -> runtime.dispatch(stage, event.scope(), valuesOf(stage, event)));
        } catch (RuntimeException thrown) {
            ScriptRefusedException tooLate = causeOf(thrown, ScriptRefusedException.class);

            if (tooLate != null) {
                // ⚠️ A rule refusing at a moment that cannot refuse. The registry stops a DECISION stage
                // being declared on an unrefusable event, but nothing stops an author calling
                // @decision.refuse in a handler for an observation — and silence there would leave them
                // believing they had prevented something that had already happened.
                LOGGER.warn("rule at '{}' tried to refuse, but this moment is only watched — it has "
                                    + "already happened. The refusal said: {}", stage.name(), tooLate.getMessage());

                return;
            }

            LOGGER.warn("rule at '{}' failed and was ignored", stage.name(), thrown);
        }
    }

    /**
     * ⚠️ Walks the cause chain, and stops on a self-referencing cause.
     *
     * <p>A cause that points at itself is legal and rare, and a loop that does not check for it hangs
     * the thread it is on rather than failing.</p>
     */
    private static <T extends Throwable> T causeOf(Throwable thrown, Class<T> wanted) {
        for (Throwable found = thrown; found != null; found = found.getCause()) {
            if (wanted.isInstance(found)) {
                return wanted.cast(found);
            }

            if (found.getCause() == found) {
                return null;
            }
        }

        return null;
    }

    /** The stage for this event, but only when it is the timing this verb serves. */
    private ScriptStage<?> stageAt(DomainEvent event, StageTiming timing) {
        ScriptStage<?> stage = registry.stageFor(event);

        return stage != null && stage.timing() == timing ? stage : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> valuesOf(ScriptStage<?> stage, DomainEvent event) {
        return ((ScriptStage<DomainEvent>) stage).bind(event);
    }

}
