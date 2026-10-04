package org.jmouse.script.spring;

import org.jmouse.script.StageDispatcher;
import org.jmouse.script.event.AmendableEvent;
import org.jmouse.script.event.DecisionEvent;
import org.jmouse.script.event.DomainEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Where Spring's events meet the rule engine.
 *
 * <h2>⚠️ THREE METHODS, AND THEY WERE ABOUT TO BE WRITTEN ONCE PER PRODUCT</h2>
 *
 * <p>{@code jmouse-script} depends on no container, so nothing in it can be annotated and nothing in it
 * listens. What it does know is what each verb means and when each may be used. This class supplies the
 * one thing only a Spring application can — <em>how an application announces that a moment has
 * arrived</em> — and it is identical in every such application, which is why it is here rather than
 * in each of them.</p>
 *
 * <p>The property worth having survives: a product publishes facts about itself and never learns that
 * anything listens. Drop this library and the product still compiles and still behaves identically,
 * with no rules running.</p>
 *
 * <h2>⚠️ THE ANNOTATIONS ARE NOT INTERCHANGEABLE</h2>
 *
 * <p>Two of the verbs run <b>before</b> the thing they are about and must therefore be plain
 * {@code @EventListener} — synchronous, on the caller's thread, inside whatever transaction the caller
 * has. Observation runs <b>after commit</b>, which is the only point at which "it happened" is true.</p>
 *
 * <p>⚠️ {@code fallbackExecution = true} is load-bearing. {@code @TransactionalEventListener} does
 * nothing at all when there is no transaction — not "runs immediately", <em>nothing</em> — and much of
 * what a product wants watched runs on an executor outside one. Without it, every observation rule
 * raised from background work would silently never fire, and the screen would show the rule as
 * healthy.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptEventBridge {

    private final StageDispatcher dispatcher;

    public ScriptEventBridge(StageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /** Rules that may refuse: synchronously, before the thing they are about. */
    @EventListener
    public void decide(DecisionEvent event) {
        dispatcher.decide(event);
    }

    /** Rules that change the outcome: synchronously, while it can still be changed. */
    @EventListener
    public void amend(AmendableEvent event) {
        dispatcher.amend(event);
    }

    /** Rules that only watch: after the commit, because that is when it is true. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void observe(DomainEvent event) {
        dispatcher.observe(event);
    }

}
