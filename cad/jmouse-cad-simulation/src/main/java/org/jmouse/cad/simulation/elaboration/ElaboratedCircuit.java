package org.jmouse.cad.simulation.elaboration;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Parameter;
import org.jmouse.cad.Setting;
import org.jmouse.cad.Text;
import org.jmouse.cad.component.Component;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.ParameterEvaluation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A circuit as the engine simulates it, worked out once from what was written ({@link CircuitElaboration}): its parts,
 * each its component compiled and its pins wired, the circuit's own parameters evaluated, and the bench it runs on. A
 * netlist, a part's limits, its displays and its instruments are all built from this one — none of them reads a
 * document or applies a bench again — and every one of them reads a part's parameters by the one rule here.
 *
 * @param elements what the bench put in that no document writes: a failed part's stand-ins, a source's offset
 * @param values   the circuit's own parameters, in SI, in the order written: what a part's settings are read over
 * @param duration how long it runs for, seconds
 * @param written  the circuit as it was written, before the bench did anything to it: what a part's own readings, limits
 *                 and displays are read over
 * @param assemblies the parts on the sheet simulated as the parts they are made of: what they report is theirs, added up
 */
public record ElaboratedCircuit(List<ElaboratedPart> parts, List<EngineElement> elements, Map<String, Double> values,
                                BenchConditions bench, double duration, Written written, List<Assembly> assemblies) {

    /** What joins a block's name to what is its own: {@code A.output}, {@code A.V_input_offset}. */
    public static final String UNIT_SEPARATOR = ".";

    public ElaboratedCircuit {
        parts = List.copyOf(parts);
        elements = List.copyOf(elements);
        assemblies = List.copyOf(assemblies);
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /**
     * The circuit as written, before its bench: every part — a failed one among them, as it was before it failed — and
     * what a bench fixture put beside them.
     */
    public record Written(List<ElaboratedPart> parts, List<EngineElement> fixtures) {

        public Written {
            parts = List.copyOf(parts);
            fixtures = List.copyOf(fixtures);
        }
    }

    /** One piece of this circuit — some of its parts and elements — on the same bench, over the same parameters. */
    public ElaboratedCircuit piece(List<ElaboratedPart> pieceParts, List<EngineElement> pieceElements) {
        return new ElaboratedCircuit(pieceParts, pieceElements, values, bench, duration, written, assemblies);
    }

    /** The part of that reference; refused when the circuit has none. */
    public ElaboratedPart part(String reference) {
        return parts.stream().filter(part -> part.reference().equals(reference)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The circuit has no part '" + reference + "'."));
    }

    /**
     * The parameters one block of a part is read with, by its equations, its heat law, its instruments, its displays and
     * its limits alike: in order, the bench's figures, the component's own parameters, what the part sets — a setting
     * {@code V_input_offset} is every block's, {@code A.V_input_offset} block {@code A}'s alone —, then what the bench
     * sets over them.
     *
     * @param unit the block, or "" for the part as a whole — which reads no block's own setting
     * @throws IllegalArgumentException for a setting of a parameter the component does not have
     */
    public Map<String, Double> parameters(ElaboratedPart part, String unit) {
        Map<String, Double> parameters = bench.figures();
        for (EvaluatedParameter parameter : part.component().parameters()) {
            parameters.put(parameter.name(), parameter.value());
        }
        Map<String, String> texts = texts(part);
        for (Setting setting : part.settings()) {
            String key = setting.name();
            int separator = key.indexOf(UNIT_SEPARATOR);
            String name = separator < 0 ? key : key.substring(separator + 1);
            if (texts.containsKey(name)) {
                // A text is not a number: it is the part's, read by its behaviour, never by its equations.
                continue;
            }
            if (!parameters.containsKey(name)) {
                throw new IllegalArgumentException("'" + part.reference() + "': " + part.component().title()
                        + " has no parameter '" + name + "' to set.");
            }
            boolean own = separator < 0 || (!unit.isEmpty() && key.substring(0, separator).equals(unit));
            if (own) {
                parameters.put(name, value(part, name, setting));
            }
        }
        parameters.putAll(part.conditioned());
        return parameters;
    }

    /**
     * A part's text parameters — read by its behaviour, never by its equations —: its components', the most general
     * first, with what the part writes over them.
     *
     * @throws IllegalArgumentException when a part sets a text parameter to something other than a text
     */
    public Map<String, String> texts(ElaboratedPart part) {
        Map<String, String> texts = new LinkedHashMap<>();
        for (Component component : part.component().lineage()) {
            for (Parameter parameter : component.parameters()) {
                if (parameter.value() instanceof Text text) {
                    texts.put(parameter.name(), text.text());
                }
            }
        }
        for (Setting setting : part.settings()) {
            if (!texts.containsKey(setting.name())) {
                continue;
            }
            if (!(setting.value() instanceof Text text)) {
                throw new IllegalArgumentException(part.reference() + ": its " + setting.name() + " is a text, written in quotes: "
                        + setting.name() + ": \"…\"");
            }
            texts.put(setting.name(), TextInterpolation.interpolated(part.reference() + ": its " + setting.name(), text.text(), values));
        }
        return texts;
    }

    private double value(ElaboratedPart part, String name, Setting setting) {
        if (!(setting.value() instanceof Formula formula)) {
            throw new IllegalArgumentException(part.reference() + ": its " + name + " is a number, not a text");
        }
        return ParameterEvaluation.value(part.reference() + ": its " + name, formula, values);
    }
}
