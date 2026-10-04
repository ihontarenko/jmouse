package org.jmouse.script.spi;

import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.event.DomainEvent;
import org.jmouse.script.stage.StageTiming;

import java.util.List;
import java.util.Map;

/**
 * One moment a rule may run at: a domain event, the name a script writes after {@code on}, and what the
 * handler is handed.
 *
 * <h2>⚠️ A DECLARATION, NEVER A CALL</h2>
 *
 * <p>This is the whole difference from the shape a rule engine usually grows into. The tempting one is
 * an adapter class per place, each calling {@code runtime.dispatch(...)} by hand from inside the code it
 * observes — which works, and which spreads the rule engine across the code it is supposed to watch.
 * Here the product publishes a fact about itself and never learns that anything listens; a stage is a
 * declaration that says <em>this event is also a moment for rules, and it is called this</em>.</p>
 *
 * <p>The consequence is the property worth having: <b>remove the scripting module and the product still
 * compiles and still behaves identically.</b></p>
 *
 * <h2>⚠️ THE NAME IS A CONTRACT WITH EVERY SCRIPT ALREADY WRITTEN</h2>
 *
 * <p>Renaming a stage stops every handler written against the old name from firing. The binder refuses
 * an unknown name when a document is loaded, so it is caught — but only on the next load, which may be a
 * restart away. Treat a stage name the way you would a column name.</p>
 *
 * <h2>⚠️ ADDING ONE EDITS NOTHING</h2>
 *
 * <p>A stage is supplied to the registry, not listed in it. There is no enum, no central list and no
 * registration file — so the day a product grows a new pipeline, its stage arrives as a class in the
 * package that owns that pipeline, and nothing in this library changes.</p>
 *
 * @param <E> the domain event this stage is raised from
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ScriptStage<E extends DomainEvent> {

    /**
     * What a script writes after {@code on} — {@code "matchSettled"}.
     *
     * <p>⚠️ Subject first: {@code matchSettled}, {@code postWritten}, {@code itemSeen}. A completion
     * list sorted alphabetically then groups by subject on its own, which is how somebody finds the
     * stage they want without knowing its name.</p>
     *
     * @return the stage name, unique across the installation
     */
    String name();

    /**
     * The domain event this stage is raised from.
     *
     * <p>⚠️ Matched exactly, not by assignability. A stage declared on a supertype would fire for every
     * subtype and hand each of them to a {@link #bind} written for one — and the failure would be a rule
     * quietly running against events it was never meant to see.</p>
     *
     * @return the event class
     */
    Class<E> event();

    /**
     * What the handler is handed, by name.
     *
     * <h2>⚠️ ALWAYS EVERY DECLARED NAME, even where the value is null</h2>
     *
     * <p>A guard that reads a name the host left out answers false rather than throwing, so a stage that
     * omits a key on some paths produces rules that silently stop running under conditions nobody can
     * see. Bind every name every time; {@code null} is a value, absence is a bug.</p>
     *
     * @param event what happened
     * @return the names a script may read, and what is behind each
     */
    Map<String, Object> bind(E event);

    /**
     * Every name this stage publishes, in the order somebody reads them.
     *
     * <h2>⚠️ IT MUST BE THE SAME LIST {@link #bind} PRODUCES, AND THE ONLY SAFE WAY IS TO DERIVE IT</h2>
     *
     * <p>The editor offers these for completion and writes them into a new document's starter body. Kept
     * as a second list beside the binding they drift, and the failure is somebody writing a rule against
     * a name the editor offered and the stage does not bind: it loads, it runs, it reads nothing, and its
     * guard answers false for ever with no error anywhere.
     * {@link org.jmouse.script.stage.StageBinding} holds the readers so the names <em>are</em> the
     * bindings.</p>
     *
     * @return the names a rule may read
     */
    List<String> names();

    /**
     * An example {@code when} clause for this moment.
     *
     * <h2>⚠️ IT GOES INTO EVERY NEW DOCUMENT, AND THAT IS DELIBERATE</h2>
     *
     * <p>A rule created with an empty body is a rule that runs on everything — every item in every walk —
     * until its author remembers to narrow it. Starting from a guard makes the cheap filter the default
     * and the expensive one a decision.</p>
     *
     * @return a guard expression, valid at this stage
     */
    String guard();

    /**
     * Whether this stage may refuse what is about to happen, change it, or only watch it.
     *
     * <p>⚠️ Defaults to watching. A stage that can refuse must run inside the caller's thread before the
     * consequence, and that is a cost and a risk to opt into deliberately.</p>
     *
     * @return when this stage runs
     */
    default StageTiming timing() {
        return StageTiming.OBSERVATION;
    }

    /**
     * How far a rule may go at this stage.
     *
     * <h2>⚠️ PER STAGE, NOT ONE FOR THE INSTALLATION</h2>
     *
     * <p>A single ceiling is right where every script runs on a request thread inside a write. It is not
     * right where one rule runs once per item in a walk of thousands and another runs once after a
     * network fetch that already took a second: generous enough for the second is a walk that takes an
     * extra hour, and tight enough for the first is a rule that cannot finish.</p>
     *
     * <p>See {@link org.jmouse.script.ScriptBudgets} for the four this library names.</p>
     *
     * @return the ceiling for this stage
     */
    ScriptBudget ceiling();

    /**
     * A phrase for the editor's completion list — what this moment is, in a few words.
     *
     * <p>Documentation for whoever writes a rule; it reaches nothing at run time.</p>
     *
     * @return a short description, or {@code null}
     */
    default String detail() {
        return null;
    }

}
