package org.jmouse.script;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.script.el.host.BoundScript;
import org.jmouse.script.el.host.ScriptHost;
import org.jmouse.script.spi.ScriptDocuments;
import org.jmouse.script.spi.ScriptStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * The one place rules are actually run.
 *
 * <h2>⚠️ IT DOES NOT DECIDE WHEN</h2>
 *
 * <p>The clock, the threads and the transaction are {@link org.jmouse.script.stage.StageDispatcher}'s.
 * This class is given a stage and some values and runs whatever is assigned there — the same division
 * the dialect itself draws, and the reason this is testable without a container.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptRuntime.class);

    private final ScriptHost      host;
    private final ScriptDocuments documents;

    /**
     * ⚠️ THE STORE IS PASSED IN, AND {@link ScriptDocuments#none()} IS THE ANSWER WHEN THERE IS NONE.
     *
     * <p>That fallback is what lets the seams exist before storage does: no store, nothing to run, and
     * the product behaves exactly as it did before rules.</p>
     *
     * <p>⚠️ A WARNING FOR WHOEVER WIRES THIS. Where the container offers a "use this bean only if no
     * other exists" conditional, resist it: those are evaluated in an order nobody controls, so one can
     * decide "there is no store" while the store is a bean two classes away — and the symptom is an
     * installation whose rules all silently do nothing. Resolve the fallback once, after every component
     * is known, and hand the answer here.</p>
     *
     * @param host      the dialect's host
     * @param documents what is assigned where, or {@link ScriptDocuments#none()}
     */
    public ScriptRuntime(ScriptHost host, ScriptDocuments documents) {
        this.host = host;
        this.documents = documents == null ? ScriptDocuments.none() : documents;
    }

    /**
     * The host, for whoever loads, validates or edits a document.
     *
     * @return the host
     */
    public ScriptHost getHost() {
        return host;
    }

    /**
     * Runs every rule assigned to one stage, in one tenant.
     *
     * <p>⚠️ <strong>The scope is the EVENT'S, never the caller's idea of where it is.</strong> It
     * arrives on the event because the event is the only thing that knows — a stage is a declaration
     * shared by every tenant, and a dispatcher reading an ambient context would be right on a request
     * thread and wrong in exactly the place it is hardest to notice: an observation, which runs after
     * commit on an executor where there is no context at all.
     *
     * @param stage  the stage
     * @param scope  the tenant, or {@code null} for the installation as a whole
     * @param values what the stage bound, by name
     * @return how many handlers ran
     * @throws ScriptRefusedException when a rule objected; the caller decides what that means
     */
    public int dispatch(ScriptStage<?> stage, String scope, Map<String, Object> values) {
        List<BoundScript> assigned = documents.forStage(scope, stage.name());

        if (assigned.isEmpty()) {
            return 0;
        }

        int ran = 0;

        for (BoundScript script : assigned) {
            /*
              ⚠️ BEFORE A CONTEXT IS BUILT, and this cheap filter is what makes the whole thing
              affordable. Assembling a context costs a scope chain and a lookup per declared function,
              and a per-item stage asks this question once per item in a walk of thousands. A document
              assigned to a stage it holds no handler for must cost a map lookup, not a scope chain.
             */
            if (script.handlersFor(stage.name()).isEmpty()) {
                continue;
            }

            ran += run(script, stage.name(), scope, values);
        }

        return ran;
    }

    /**
     * Runs ONE document at a stage — the rule a declared step names.
     *
     * @param stage        the stage
     * @param scope        the tenant, or {@code null} for the installation as a whole
     * @param documentName which rule
     * @param values       what the stage bound, by name
     * @return 1 if it ran, 0 if no bound, enabled document of that name handles the stage
     */
    public int dispatchOne(ScriptStage<?> stage, String scope, String documentName, Map<String, Object> values) {
        for (BoundScript script : documents.forStage(scope, stage.name())) {
            if (script.getName().equals(documentName) && !script.handlersFor(stage.name()).isEmpty()) {
                return run(script, stage.name(), scope, values);
            }
        }

        return 0;
    }

    private int run(BoundScript script, String stage, String scope, Map<String, Object> values) {
        EvaluationContext context = host.newContext(script);

        values.forEach(context::setValue);

        LOGGER.debug("running '{}' at '{}'", script.getName(), stage);

        AmendmentScope.running(script.getName(), stage, scope);

        try {
            return host.dispatch(script, stage, context);
        } finally {
            AmendmentScope.running(null, null, null);
        }
    }

}
