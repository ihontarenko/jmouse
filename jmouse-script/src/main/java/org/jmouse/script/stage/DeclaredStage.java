package org.jmouse.script.stage;

import org.jmouse.script.el.budget.ScriptBudget;
import org.jmouse.script.event.AmendableEvent;
import org.jmouse.script.event.DecisionEvent;
import org.jmouse.script.event.DomainEvent;
import org.jmouse.script.spi.ScriptStage;

import java.util.List;
import java.util.Map;

/**
 * A stage declared as data: a name, an event, a binding, a timing, a ceiling and an example guard.
 *
 * <h2>⚠️ ONE IMPLEMENTATION RATHER THAN FOURTEEN NEARLY IDENTICAL ONES</h2>
 *
 * <p>Every stage a product has differs from every other in exactly those things. Written as a class
 * each, the differences would be five lines apiece surrounded by forty lines of the same boilerplate —
 * and boilerplate is where a {@code timing()} gets forgotten and a stage that was meant to be able to
 * refuse silently becomes one that only watches.</p>
 *
 * <p>⚠️ This is <strong>not</strong> a central registry, and the distinction matters: a stage is still
 * declared by whoever owns the moment, and adding one still edits no shared file. What is shared here is
 * the shape, not the list.</p>
 *
 * <h2>⚠️ USE THE THREE FACTORIES, NOT THE CONSTRUCTOR</h2>
 *
 * <p>Each one bounds its event type to what that timing needs, so a stage that could refuse an event
 * with nowhere to put a refusal <b>does not compile</b>. The constructor cannot express that, which is
 * why the registry keeps a startup check for the case somebody uses it anyway.</p>
 *
 * @param <E> the domain event this stage is raised from
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public record DeclaredStage<E extends DomainEvent>(
        String name,
        Class<E> event,
        StageBinding<E> binding,
        StageTiming timing,
        ScriptBudget ceiling,
        String detail,
        String guard
) implements ScriptStage<E> {

    @Override
    public Map<String, Object> bind(E event) {
        return binding.read(event);
    }

    @Override
    public List<String> names() {
        return binding.names();
    }

    /**
     * A stage that watches something that has already happened.
     *
     * @param name    what a rule writes after {@code on}
     * @param event   the event it is raised from
     * @param ceiling how far a rule may go here
     * @param detail  a phrase for the editor's completion list
     * @param guard   an example {@code when} clause, for a new document's starter body
     * @param binding what a rule is handed
     * @param <E>     the event type
     * @return the stage
     */
    public static <E extends DomainEvent> ScriptStage<E> observation(
            String name, Class<E> event, ScriptBudget ceiling, String detail, String guard,
            StageBinding<E> binding) {

        return new DeclaredStage<>(name, event, binding, StageTiming.OBSERVATION, ceiling, detail, guard);
    }

    /**
     * A stage whose outcome a rule may change — see {@link StageTiming#AMENDMENT}.
     *
     * <p>⚠️ Bounded to {@link AmendableEvent}, because an amendment has to be recorded somewhere and an
     * event without that has nowhere to put it.</p>
     *
     * @param name    what a rule writes after {@code on}
     * @param event   the event it is raised from
     * @param ceiling how far a rule may go here
     * @param detail  a phrase for the editor's completion list
     * @param guard   an example {@code when} clause, for a new document's starter body
     * @param binding what a rule is handed
     * @param <E>     the event type
     * @return the stage
     */
    public static <E extends AmendableEvent> ScriptStage<E> amendment(
            String name, Class<E> event, ScriptBudget ceiling, String detail, String guard,
            StageBinding<E> binding) {

        return new DeclaredStage<>(name, event, binding, StageTiming.AMENDMENT, ceiling, detail, guard);
    }

    /**
     * A stage that runs before the consequence and may refuse it.
     *
     * <p>⚠️ Bounded to {@link DecisionEvent} here rather than only checked at startup, so a stage that
     * can refuse an event carrying nowhere to put a refusal does not compile. The startup check in the
     * registry stays for the case this signature cannot see — a stage built through the record
     * constructor directly.</p>
     *
     * @param name    what a rule writes after {@code on}
     * @param event   the event it is raised from
     * @param ceiling how far a rule may go here
     * @param detail  a phrase for the editor's completion list
     * @param guard   an example {@code when} clause, for a new document's starter body
     * @param binding what a rule is handed
     * @param <E>     the event type
     * @return the stage
     */
    public static <E extends DecisionEvent> ScriptStage<E> decision(
            String name, Class<E> event, ScriptBudget ceiling, String detail, String guard,
            StageBinding<E> binding) {

        return new DeclaredStage<>(name, event, binding, StageTiming.DECISION, ceiling, detail, guard);
    }

}
