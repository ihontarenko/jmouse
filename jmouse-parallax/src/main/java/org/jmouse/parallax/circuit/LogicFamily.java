package org.jmouse.parallax.circuit;

import org.jmouse.parallax.component.ComponentModel;
import org.jmouse.parallax.component.ModelLanguage;

import org.jmouse.formula.Formula;
import org.jmouse.formula.RealCompiler;
import org.jmouse.formula.RealFunction;
import org.jmouse.formula.Symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * A logic family as its {@code family} document writes it — the laws every part of it shares, each a formula of the
 * supply its part runs from, and the figures of its pins:
 *
 * <pre>
 * input_low(supply), input_high(supply)   the thresholds an input reads a node by, volts: a share of the supply for
 *                                         CMOS, fixed volts for TTL; between them an input holds what it last was
 * output_resistance(supply)               what an output pulls its pin to a rail through, ohms
 * delay_scale(supply)                     how much slower every delay is at this supply than as a part writes it
 * minimum_supply                          below this the part is not powered: its inputs read unknown
 * edge                                    an output's transition time
 * input_capacitance, output_capacitance   each pin's capacitance to ground
 * </pre>
 *
 * Compiled once per family; the engine only evaluates it. A family that leaves one out is refused, naming what it
 * lacks — a digital part never runs on figures nobody wrote.
 */
final class LogicFamily {

    private static final String SUPPLY = "supply";
    private static final List<String> LAWS = List.of("input_low", "input_high", "output_resistance", "delay_scale");
    private static final List<String> FIGURES = List.of("minimum_supply", "edge", "input_capacitance", "output_capacitance");

    private final RealFunction inputLow;
    private final RealFunction inputHigh;
    private final RealFunction outputResistance;
    private final RealFunction delayScale;
    private final double minimumSupply;
    private final double edge;
    private final double inputCapacitance;
    private final double outputCapacitance;
    /** The supply first, then the family's parameters: one argument array, read by every law. */
    private final double[] arguments;

    private LogicFamily(ComponentModel family) {
        Map<String, Double> values = new LinkedHashMap<>();
        family.parameters().forEach(parameter -> values.put(parameter.name(), parameter.value()));
        List<String> missing = new ArrayList<>();
        FIGURES.stream().filter(figure -> !values.containsKey(figure)).forEach(missing::add);
        List<Formula.Definition> library = ModelLanguage.LANGUAGE.definitions(ModelLanguage.libraryText(family));
        LAWS.stream().filter(law -> library.stream().noneMatch(definition -> definition.name().equals(law)
                && definition.parameters().size() == 1)).map(law -> law + "(supply)").forEach(missing::add);
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("The logic family '" + family.name() + "' does not write "
                    + String.join(", ", missing) + ": a digital part runs on what its family writes, nothing else.");
        }
        List<String> parameterNames = List.copyOf(values.keySet());
        Symbols symbols = Symbols.of(List.of(SUPPLY), parameterNames);
        this.inputLow = law(family, "input_low", library, symbols);
        this.inputHigh = law(family, "input_high", library, symbols);
        this.outputResistance = law(family, "output_resistance", library, symbols);
        this.delayScale = law(family, "delay_scale", library, symbols);
        this.minimumSupply = values.get("minimum_supply");
        this.edge = values.get("edge");
        this.inputCapacitance = values.get("input_capacitance");
        this.outputCapacitance = values.get("output_capacitance");
        this.arguments = new double[1 + parameterNames.size()];
        for (int index = 0; index < parameterNames.size(); index++) {
            arguments[1 + index] = values.get(parameterNames.get(index));
        }
    }

    /** The family a digital part names, compiled. */
    static LogicFamily of(ComponentModel family) {
        return new LogicFamily(family);
    }

    private static RealFunction law(ComponentModel family, String name, List<Formula.Definition> library, Symbols symbols) {
        String call = name + "(" + SUPPLY + ")";
        return RealCompiler.compile(ModelLanguage.read(family.name(), call, 0, library), symbols, family.name() + ": " + call);
    }

    private synchronized double at(RealFunction law, double supply) {
        arguments[0] = supply;
        return law.evaluate(arguments);
    }

    double inputLow(double supply) {
        return at(inputLow, supply);
    }

    double inputHigh(double supply) {
        return at(inputHigh, supply);
    }

    double outputResistance(double supply) {
        return at(outputResistance, supply);
    }

    double delayScale(double supply) {
        return at(delayScale, supply);
    }

    /** Whether a part across these rails is powered at all. */
    boolean powers(double supply) {
        return supply >= minimumSupply;
    }

    double edge() {
        return edge;
    }

    double inputCapacitance() {
        return inputCapacitance;
    }

    double outputCapacitance() {
        return outputCapacitance;
    }
}
