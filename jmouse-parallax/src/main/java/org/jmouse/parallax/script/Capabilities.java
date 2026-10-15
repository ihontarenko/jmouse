package org.jmouse.parallax.script;

import org.jmouse.parallax.script.Block.ParameterFlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * What a document can do, read off its structure. Nothing here is declared by the author, so nothing here can be
 * claimed falsely: a time slider appears for an object whose time is an input, Run for one whose state evolves, and
 * neither for a saddle.
 *
 * <table>
 *   <tr><th>Capability</th><th>Derived from</th></tr>
 *   <tr><td>{@link Capability#PLAY}</td><td>a parameter marked {@code animate}, or an input called {@code t}</td></tr>
 *   <tr><td>{@link Capability#RUN}</td><td>a state with {@code d(x)/dt} or {@code x_next}, or a transient analysis —
 *   but not a state iterated at every point of a complex input, whose orbit is folded into what each point shows</td></tr>
 *   <tr><td>{@link Capability#INTERACT}</td><td>a parameter marked {@code live}, or a part whose component has one</td></tr>
 *   <tr><td>{@link Capability#RESAMPLE}</td><td>an expression calling a {@code random_} function or {@code white_noise}</td></tr>
 *   <tr><td>{@link Capability#PROBE}</td><td>a circuit with parts</td></tr>
 *   <tr><td>{@link Capability#SWEEP}</td><td>a parameter with a range</td></tr>
 *   <tr><td>{@link Capability#CHECK}</td><td>an {@code expect} block</td></tr>
 *   <tr><td>{@link Capability#EXECUTE}</td><td>a {@code run} block</td></tr>
 * </table>
 *
 * <p>{@code view { without … }} removes a capability; there is no syntax that adds one.</p>
 */
public final class Capabilities {

    private static final Pattern RANDOM = Pattern.compile("\\b(?:random_\\w*|white_noise)\\s*\\(");

    private Capabilities() {
    }

    /** The document's own capabilities. */
    public static Set<Capability> of(ExperienceDocument document) {
        return of(document, reference -> null);
    }

    /**
     * The capabilities of a document whose parts are resolved: a circuit becomes interactive when one of its parts'
     * components has a {@code live} parameter.
     *
     * @param components the document a part's reference names, or {@code null} when it is not known
     */
    public static Set<Capability> of(ExperienceDocument document, Function<String, ExperienceDocument> components) {
        if (document.kind().role() != Kind.Role.COMPUTABLE) {
            return Set.of();
        }
        EnumSet<Capability> capabilities = EnumSet.noneOf(Capability.class);
        if (document.parameters().stream().anyMatch(parameter -> parameter.has(ParameterFlag.ANIMATE))
                || timeIsAnInput(document)) {
            capabilities.add(Capability.PLAY);
        }
        if (evolves(document)) {
            capabilities.add(Capability.RUN);
        }
        if (document.parameters().stream().anyMatch(parameter -> parameter.has(ParameterFlag.LIVE))
                || partsAreLive(document, components)) {
            capabilities.add(Capability.INTERACT);
        }
        if (drawsOnRandomness(document)) {
            capabilities.add(Capability.RESAMPLE);
        }
        if (document.kind() == Kind.CIRCUIT && document.block(Block.Parts.class).isPresent()) {
            capabilities.add(Capability.PROBE);
        }
        if (document.parameters().stream().anyMatch(parameter -> parameter.range().isPresent())) {
            capabilities.add(Capability.SWEEP);
        }
        if (!document.blocks(Block.Expectation.class).isEmpty()) {
            capabilities.add(Capability.CHECK);
        }
        if (document.block(Block.Run.class).isPresent()) {
            capabilities.add(Capability.EXECUTE);
        }
        document.block(Block.View.class).ifPresent(view -> view.items().stream()
                .filter(Block.Without.class::isInstance).map(Block.Without.class::cast)
                .forEach(without -> capabilities.removeAll(without.capabilities())));
        return Set.copyOf(capabilities);
    }

    /**
     * Time evolves state: a derivative or a step of a state, events in a behaviour, or a transient analysis. A state
     * stepped from every point of a complex input — {@code (c) in …} with {@code z_next = z^2 + c} — does not evolve
     * in time: each point's orbit is run to its end and folded into the picture.
     */
    public static boolean evolves(ExperienceDocument document) {
        boolean stateEquation = document.block(Block.Equations.class).stream()
                .flatMap(equations -> equations.equations().stream())
                .anyMatch(equation -> equation.form() == Block.EquationForm.DERIVATIVE
                        || equation.form() == Block.EquationForm.NEXT);
        boolean transientAnalysis = document.blocks(Block.Analysis.class).stream()
                .anyMatch(analysis -> analysis.kind() == Block.AnalysisKind.TRANSIENT);
        boolean events = document.block(Block.Behaviour.class).isPresent();
        return (stateEquation && !iteratedOverThePlane(document)) || transientAnalysis || events;
    }

    private static boolean iteratedOverThePlane(ExperienceDocument document) {
        return document.block(Block.Inputs.class).stream().flatMap(inputs -> inputs.inputs().stream())
                .anyMatch(Block.Input::complex);
    }

    private static boolean timeIsAnInput(ExperienceDocument document) {
        return document.block(Block.Inputs.class).stream().flatMap(inputs -> inputs.inputs().stream())
                .anyMatch(input -> input.names().contains("t")) && !evolves(document);
    }

    private static boolean partsAreLive(ExperienceDocument document, Function<String, ExperienceDocument> components) {
        return document.block(Block.Parts.class).stream().flatMap(parts -> parts.parts().stream())
                .map(part -> components.apply(part.reference()))
                .anyMatch(component -> component != null && component.parameters().stream()
                        .anyMatch(parameter -> parameter.has(ParameterFlag.LIVE)));
    }

    private static boolean drawsOnRandomness(ExperienceDocument document) {
        return expressions(document).stream().anyMatch(text -> RANDOM.matcher(text).find());
    }

    /** Every expression the document holds, as text. */
    private static Collection<String> expressions(ExperienceDocument document) {
        List<String> texts = new ArrayList<>();
        for (Block block : document.blocks()) {
            switch (block) {
                case Block.Outputs outputs -> outputs.outputs().forEach(output -> {
                    output.components().forEach(component -> texts.add(component.text()));
                    output.equation().ifPresent(equation -> texts.add(equation.text()));
                });
                case Block.Equations equations -> equations.equations()
                        .forEach(equation -> texts.add(equation.expression().text()));
                case Block.Formulas formulas -> formulas.definitions().ifPresent(definitions -> texts.add(definitions.text()));
                case Block.Run run -> texts.add(run.source().text());
                case Block.Maps maps -> maps.maps().forEach(map -> map.components().forEach(component -> texts.add(component.text())));
                default -> {
                }
            }
        }
        return texts;
    }
}
