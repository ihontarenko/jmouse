package org.jmouse.script.stage;

import org.jmouse.script.event.AmendableEvent;
import org.jmouse.script.event.DecisionEvent;
import org.jmouse.script.event.DomainEvent;
import org.jmouse.script.spi.ScriptStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every stage this build declares, indexed by its event and by its name.
 *
 * <h2>⚠️ IT TAKES THE LIST; IT DOES NOT GO AND FIND ONE</h2>
 *
 * <p>Whoever builds this hands over every stage on the classpath — a container can do it in one line,
 * and a product without one can pass a list literal. Either way the registry is the same object, and
 * this library depends on no container to have it. That is the convention the rest of jMouse keeps and
 * the one thing this mechanism did <b>not</b> keep where it came from.</p>
 *
 * <h2>⚠️ THREE CHECKS AT CONSTRUCTION, AND EACH ONE FAILS THE BUILD RATHER THAN A REQUEST</h2>
 *
 * <p>All three describe the same class of fault: a rule that loads, runs, and quietly means nothing.
 * Caught here they are a startup error naming both offenders; caught later they are a person certain
 * they stopped something that went ahead.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class StageRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(StageRegistry.class);

    private final Map<Class<?>, ScriptStage<?>> byEvent;
    private final Map<String, ScriptStage<?>>   byName;

    public StageRegistry(List<ScriptStage<?>> declared) {
        this.byEvent = new LinkedHashMap<>();
        this.byName = new LinkedHashMap<>();

        List<ScriptStage<?>> ordered = declared.stream()
                .sorted(Comparator.comparing(ScriptStage::name))
                .toList();

        for (ScriptStage<?> stage : ordered) {
            requireTimingTheEventCanCarry(stage);
            requireUniqueEvent(stage);
            requireUniqueName(stage);

            byEvent.put(stage.event(), stage);
            byName.put(stage.name(), stage);
        }

        LOGGER.info("jMS stages ready: {}", byName.keySet());
    }

    /**
     * The stage raised from this event.
     *
     * <p>⚠️ Looked up by the EXACT class, never by assignability. A stage found for a subclass it was
     * not written for would be handed an event its binding cannot read.</p>
     *
     * @param event what happened
     * @return the stage, or {@code null} when nothing declares this event
     */
    public ScriptStage<?> stageFor(DomainEvent event) {
        return byEvent.get(event.getClass());
    }

    /**
     * The stage a script names after {@code on}.
     *
     * @param name the stage name
     * @return the stage, or {@code null} when this build declares no such name
     */
    public ScriptStage<?> stageNamed(String name) {
        return byName.get(name);
    }

    /** Every stage, by name. */
    public List<ScriptStage<?>> all() {
        return List.copyOf(byName.values());
    }

    /** Every stage name — what an editor offers after {@code on}. */
    public List<String> names() {
        return List.copyOf(byName.keySet());
    }

    /**
     * ⚠️ A stage may not claim a timing its event carries nowhere to record.
     *
     * <p>The factories on {@link DeclaredStage} make this unrepresentable; this catches a stage built
     * through the record constructor directly, which the signature cannot see.</p>
     */
    private void requireTimingTheEventCanCarry(ScriptStage<?> stage) {
        if (stage.timing() == StageTiming.AMENDMENT
                && !AmendableEvent.class.isAssignableFrom(stage.event())) {
            throw new IllegalStateException(
                    ("Stage '%s' says a rule may change its outcome, but %s is not an AmendableEvent and "
                            + "carries nowhere to put a change — every amendment would be lost.")
                            .formatted(stage.name(), stage.event().getName()));
        }

        if (stage.timing() == StageTiming.DECISION
                && !DecisionEvent.class.isAssignableFrom(stage.event())) {
            throw new IllegalStateException(
                    ("Stage '%s' says it may refuse, but %s is not a DecisionEvent and carries nowhere "
                            + "to put a refusal. A rule written against it would run, call refuse, and be "
                            + "ignored — leaving somebody certain they had stopped something that went "
                            + "ahead. Either make the event refusable or declare the stage as an OBSERVATION.")
                            .formatted(stage.name(), stage.event().getName()));
        }
    }

    private void requireUniqueEvent(ScriptStage<?> stage) {
        ScriptStage<?> declared = byEvent.get(stage.event());

        if (declared != null) {
            throw new IllegalStateException(
                    ("Stages '%s' and '%s' are both declared on %s. One of them would win and every "
                            + "rule written against the other would never fire, with nothing anywhere to say "
                            + "so. One event is one moment.")
                            .formatted(declared.name(), stage.name(), stage.event().getName()));
        }
    }

    private void requireUniqueName(ScriptStage<?> stage) {
        ScriptStage<?> declared = byName.get(stage.name());

        if (declared != null) {
            throw new IllegalStateException(
                    ("Two declarations carry the stage '%s': %s and %s. A stage name is what every rule "
                            + "already written is bound to, so one silently winning would change what those "
                            + "rules mean. Rename one.")
                            .formatted(stage.name(),
                                       declared.getClass().getName(),
                                       stage.getClass().getName()));
        }
    }

}
