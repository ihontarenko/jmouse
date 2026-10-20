package org.jmouse.parallax.script.engine;

import org.jmouse.parallax.script.Block;
import org.jmouse.parallax.script.Block.ParameterFlag;
import org.jmouse.datamap.Quantity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The parameters of a document written without a range: {@code bailout = 2}, {@code dimension = 4 integer},
 * {@code k = 1234567890123 integer exact}. A slider always has a range, so a parameter without one is a value of the
 * definition — an engine family's constant or a program's exact integer — and the definition reader takes it from
 * here by name. One it never takes is refused: it was meant as a slider and forgot its range.
 */
final class DefinitionConstants {

    private final Map<String, Block.Parameter> parameters = new LinkedHashMap<>();
    private final Set<String>                  taken      = new LinkedHashSet<>();

    DefinitionConstants(List<Block.Parameter> rangeless) {
        rangeless.forEach(parameter -> parameters.put(parameter.name(), parameter));
    }

    /** A constant's number, refused when the document does not write it. */
    double number(String name, String what) {
        return optionalNumber(name, what).orElseThrow(() -> new IllegalArgumentException(
                what + " writes '" + name + " = …' in its parameters."));
    }

    /** A constant's whole number, refused when the document does not write it. */
    int integer(String name, String what) {
        return optionalInteger(name, what).orElseThrow(() -> new IllegalArgumentException(
                what + " writes '" + name + " = …' in its parameters."));
    }

    /** A constant's whole number, when the document writes it; a fraction is refused rather than cut. */
    Optional<Integer> optionalInteger(String name, String what) {
        return optionalNumber(name, what).map(value -> {
            if (!Notation.isInteger(value)) {
                throw new IllegalArgumentException("'" + name + "' of " + what + " is a whole number, not " + value + ".");
            }
            return value.intValue();
        });
    }

    /** A constant's number, when the document writes it. */
    Optional<Double> optionalNumber(String name, String what) {
        Block.Parameter parameter = parameters.get(name);
        if (parameter == null) {
            return Optional.empty();
        }
        taken.add(name);
        return Optional.of(parameter.quantity().map(Quantity::value)
                .orElseGet(() -> Notation.parseNumber(parameter.value().text(), "'" + name + "' of " + what)));
    }

    /** Every {@code exact} integer, its digits as written. */
    Map<String, String> exactIntegers() {
        Map<String, String> integers = new LinkedHashMap<>();
        parameters.values().stream().filter(parameter -> parameter.has(ParameterFlag.EXACT)).forEach(parameter -> {
            taken.add(parameter.name());
            integers.put(parameter.name(), parameter.value().text().replace(" ", ""));
        });
        return integers;
    }

    /** Refuses a rangeless parameter the definition did not take. */
    void requireAllTaken() {
        for (String name : parameters.keySet()) {
            if (!taken.contains(name)) {
                throw new IllegalArgumentException("Parameter '" + name + "' has no range: a slider is written '"
                        + name + " = value in minimum .. maximum'.");
            }
        }
    }
}
