package org.jmouse.script;

import org.jmouse.script.spi.RealClass;
import org.jmouse.script.spi.ScriptFacade;
import org.jmouse.script.spi.ScriptStage;
import org.jmouse.script.stage.StageRegistry;
import org.jmouse.script.stage.StageTiming;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What this build offers a rule author: every moment a rule may run at, and everything it may reach.
 *
 * <h2>⚠️ NOT {@code ScriptCatalogue} — THE LANGUAGE ALREADY HAS ONE, AND IT IS A DIFFERENT THING</h2>
 *
 * <p>{@code org.jmouse.script.el.host.ScriptCatalogue} is the <b>closed set of names a script may
 * call</b> — the security boundary itself. This is the <b>reference somebody reads before writing a
 * rule</b>. Sharing a name would put two unrelated ideas one import apart, and any file wiring both
 * would have to spell one of them out in full to say which it meant.</p>
 *
 * <h2>⚠️ THE CONTENT IS THE MECHANISM'S; THE SHAPE IS THE PRODUCT'S</h2>
 *
 * <p>Where this came from, one class both gathered these facts and built the screen's response DTOs. The
 * first half is knowledge only this library has — which stage carries which names, what its guard is,
 * what it may do, how long it has — and the second is an API shape somebody versions. So the boundary
 * runs between them: this answers {@link StageDescription} and {@link FacadeDescription}, and a product
 * maps them into whatever its screen has promised to return.</p>
 *
 * <h2>⚠️ IT IS ALSO THE HELP PANEL, WHICH MAKES IT THE ONE PLACE A MISTAKE IS VISIBLE</h2>
 *
 * <p>Everything here is read by somebody about to write a rule. A method listed that cannot be called, a
 * deadline reported wrong, a name offered that the stage does not bind — each of those is discovered as
 * a rule that does not work, by a person who has no way of knowing the catalogue was lying.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ScriptReference {

    /**
     * ⚠️ The three methods every facade has because {@link ScriptFacade} demands them. They are how the
     * catalogue finds a facade, not something a rule calls — {@code @catalogue.name()} answers the string
     * {@code "catalogue"}, which is of no use to anybody and reads like an offer.
     */
    private static final Set<String> PLUMBING = Set.of("name", "target", "detail");

    private final StageRegistry      stages;
    private final List<ScriptFacade> facades;
    private final RealClass          realClass;

    public ScriptReference(StageRegistry stages, List<ScriptFacade> facades, RealClass realClass) {
        this.stages = stages;
        this.facades = facades;
        this.realClass = realClass;
    }

    /** Every moment, in the order the registry holds them. */
    public List<StageDescription> describeStages() {
        return stages.all().stream().map(ScriptReference::describe).toList();
    }

    /** Everything a rule may reach, by name. */
    public List<FacadeDescription> describeFacades() {
        return facades.stream()
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .map(this::describe)
                .toList();
    }

    private static StageDescription describe(ScriptStage<?> stage) {
        return new StageDescription(
                stage.name(),
                stage.detail(),
                stage.timing(),
                stage.names(),
                stage.guard(),
                /*
                  ⚠️ Milliseconds, and zero for "no ceiling" rather than a null nobody renders.

                  The deadline is the figure that differs most between one moment and another — 50ms
                  inside a walk against 5s after an import — and it is the one a rule author needs before
                  they decide what to do here. A catalogue that omits it teaches everyone to write the
                  same rule everywhere and find out which moments cannot afford it by being cut off.
                 */
                stage.ceiling().deadline() == null ? 0L : stage.ceiling().deadline().toMillis());
    }

    private FacadeDescription describe(ScriptFacade facade) {
        /*
          ⚠️ THE USER CLASS, NOT THE OBJECT'S OWN.

          A facade with advised methods is handed back as a proxy — and as a proxy even from
          `return this`. Asking the object for its class once listed `Advised`, `getCallbacks`,
          `setTargetSource` and forty other pieces of plumbing on the Help panel, as if a rule could call
          them. See RealClass: unwrapping is the product's to supply, because how it is done depends on
          what did the wrapping.
         */
        Class<?> real = realClass.of(facade.target());

        List<String> callable = Arrays.stream(real.getMethods())
                .filter(method -> method.getDeclaringClass() != Object.class)
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> !Modifier.isStatic(method.getModifiers()))
                .filter(method -> !PLUMBING.contains(method.getName()))
                .map(ScriptReference::signatureOf)
                .distinct()
                .sorted()
                .toList();

        return new FacadeDescription(facade.name(), facade.detail(), callable);
    }

    /**
     * ⚠️ Simple names, and the return type LAST.
     *
     * <p>{@code place(String, String): void} is what somebody needs to write the call, with the part
     * they type first written first. Java's own order puts the return type in front, which reads
     * correctly in a source file and badly in a completion list — the names no longer line up, so
     * scanning a column of thirty methods means reading past a different-width word on every line.</p>
     *
     * <p>Fully qualified parameter names were the other option: three times as wide, wrapping in the
     * panel, and saying nothing more to anybody choosing between two methods.</p>
     */
    private static String signatureOf(Method method) {
        String parameters = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));

        return "%s(%s): %s".formatted(method.getName(), parameters, method.getReturnType().getSimpleName());
    }

    /**
     * One moment, as a rule author reads it.
     *
     * @param name       what a script writes after {@code on}
     * @param detail     what this moment is, in a few words
     * @param timing     whether a rule here may refuse, change, or only watch — ⚠️ the enum rather than
     *                   its name, because every product that renders this has to ask which one it is,
     *                   and comparing strings is how a renamed constant becomes a screen that quietly
     *                   says every moment is an observation
     * @param names      what a handler is handed
     * @param guard      an example {@code when} clause, valid here
     * @param deadlineMs how long a rule has, or {@code 0} for no ceiling
     */
    public record StageDescription(String name, String detail, StageTiming timing, List<String> names,
                                   String guard, long deadlineMs) {
    }

    /**
     * One thing a rule may reach.
     *
     * @param name     what a rule writes after the {@code @}
     * @param detail   what it is for
     * @param callable every method on it, as a signature
     */
    public record FacadeDescription(String name, String detail, List<String> callable) {
    }

}
