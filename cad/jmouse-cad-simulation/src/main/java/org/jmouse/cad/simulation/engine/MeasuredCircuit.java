package org.jmouse.cad.simulation.engine;

import org.jmouse.cad.Formula;
import org.jmouse.cad.Parameter;
import org.jmouse.cad.Range;
import org.jmouse.cad.Setting;
import org.jmouse.cad.circuit.Circuit;
import org.jmouse.cad.circuit.Part;
import org.jmouse.cad.simulation.CatalogueDocuments;
import org.jmouse.cad.simulation.EvaluatedParameter;
import org.jmouse.cad.simulation.ParameterEvaluation;
import org.jmouse.cad.simulation.elaboration.BenchConditions;
import org.jmouse.cad.simulation.elaboration.CircuitElaboration;
import org.jmouse.cad.simulation.elaboration.ElaboratedCircuit;
import org.jmouse.cad.simulation.elaboration.EngineElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A circuit as a measurement takes it: as written, with the catalogue its components are found in, the bench it stands on
 * and what a fixture adds to it — so that a measurement {@code with} other values can build it again, as written, with
 * them.
 *
 * @param fixtures what a standard bench fixture puts in beside the parts: a current into a diode, a voltage across it
 */
public record MeasuredCircuit(Circuit circuit, CatalogueDocuments catalogue, BenchConditions bench, List<EngineElement> fixtures) {

    /** What joins a part's reference to its setting in a {@code with}: {@code S1.closed}. */
    private static final String PART_SEPARATOR = ".";

    public MeasuredCircuit {
        fixtures = List.copyOf(fixtures);
    }

    /**
     * The circuit as {@code with} sets it, elaborated: a circuit's parameter — a slider — by its name, kept within its
     * range, or a part's setting as {@code part.setting} — a switch closed, a resistor changed.
     *
     * @throws IllegalArgumentException for a name that is neither, or a value outside its parameter's range
     */
    public ElaboratedCircuit elaborated(List<Setting> with) {
        Map<String, Parameter> parameters = new LinkedHashMap<>();
        circuit.parameters().forEach(parameter -> parameters.put(parameter.name(), parameter));
        Map<String, List<Setting>> partSettings = new LinkedHashMap<>();
        for (Setting setting : with) {
            int separator = setting.name().indexOf(PART_SEPARATOR);
            if (separator > 0) {
                partSettings.computeIfAbsent(setting.name().substring(0, separator), ignored -> new ArrayList<>())
                        .add(new Setting(setting.name().substring(separator + 1), setting.value()));
                continue;
            }
            Parameter parameter = parameters.get(setting.name());
            if (parameter == null) {
                throw new IllegalArgumentException("'" + setting.name() + "' is not a parameter of the circuit; a part's setting "
                        + "is written 'part.setting'");
            }
            parameters.put(parameter.name(), new Parameter(parameter.name(), setting.value(), parameter.range(), parameter.step(),
                    parameter.per(), parameter.flags(), parameter.description()));
        }
        List<Parameter> set = List.copyOf(parameters.values());
        requireWithinRanges(set, with);
        Set<String> named = new HashSet<>(partSettings.keySet());
        List<Part> parts = circuit.parts().stream().map(part -> {
            List<Setting> changed = partSettings.get(part.reference());
            if (changed == null) {
                return part;
            }
            named.remove(part.reference());
            List<Setting> settings = new ArrayList<>(part.settings());
            settings.addAll(changed);
            return new Part(part.reference(), part.component(), part.connections(), settings, part.symbol(), part.footprint());
        }).toList();
        if (!named.isEmpty()) {
            throw new IllegalArgumentException("'with' names no part of the circuit: " + String.join(", ", named));
        }
        Circuit measured = new Circuit(circuit.head(), set, parts, circuit.layout(), circuit.probes(), circuit.timeBase(),
                circuit.expectations(), circuit.analysis(), circuit.views(), circuit.frame(), circuit.boundary());
        return CircuitElaboration.of(measured, catalogue, bench, fixtures);
    }

    /** Every parameter a {@code with} set lies within the range it is offered over. */
    private void requireWithinRanges(List<Parameter> parameters, List<Setting> with) {
        Set<String> set = new HashSet<>();
        with.forEach(setting -> set.add(setting.name()));
        Map<String, Double> known = new LinkedHashMap<>();
        for (EvaluatedParameter evaluated : ParameterEvaluation.evaluate(circuit.head().title(), parameters)) {
            known.put(evaluated.name(), evaluated.value());
        }
        for (Parameter parameter : parameters) {
            if (!set.contains(parameter.name()) || !(parameter.range() instanceof Range.Between between)) {
                continue;
            }
            double value = known.get(parameter.name());
            double low = bound(between.low(), known);
            double high = bound(between.high(), known);
            if (value < low || value > high) {
                throw new IllegalArgumentException("'" + parameter.name() + "' = " + value + " is outside its range " + low
                        + " .. " + high);
            }
        }
    }

    private double bound(Formula bound, Map<String, Double> known) {
        return ParameterEvaluation.value(circuit.head().title() + ": a range", bound, known);
    }
}
