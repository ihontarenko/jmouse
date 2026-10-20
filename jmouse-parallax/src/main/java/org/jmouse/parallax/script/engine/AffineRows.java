package org.jmouse.parallax.script.engine;

import org.jmouse.formula.AffineForm;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.el.FormulaLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * One row of an affine map written out as the formula it is. With numeric coefficients (an IFS map):
 * {@code -0.04 x + 0.85 y + 1.6}. A zero term is left out, a coefficient of one is not written, and a row with no
 * terms at all is {@code 0}. Reading is jMF's: the row is parsed as a formula and its coefficients are taken from the
 * tree ({@link AffineForm}), so every coefficient written reads back as the same {@code double}.
 */
final class AffineRows {

    private AffineRows() {
    }

    /** {@code coefficients · variables + translation}. */
    static String write(List<Double> coefficients, double translation, List<String> variables) {
        List<String> terms = new ArrayList<>();
        for (int index = 0; index < coefficients.size(); index++) {
            double coefficient = coefficients.get(index);
            if (coefficient != 0) {
                double magnitude = Math.abs(coefficient);
                terms.add(sign(coefficient) + (magnitude == 1 ? "" : Notation.number(magnitude) + " ") + variables.get(index));
            }
        }
        if (translation != 0) {
            terms.add(sign(translation) + Notation.number(Math.abs(translation)));
        }
        if (terms.isEmpty()) {
            return "0";
        }
        StringBuilder row = new StringBuilder(terms.getFirst().startsWith("-") ? "-" + terms.getFirst().substring(1)
                : terms.getFirst().substring(1));
        for (String term : terms.subList(1, terms.size())) {
            row.append(' ').append(term.charAt(0)).append(' ').append(term.substring(1));
        }
        return row.toString();
    }

    /**
     * One row of an affine map whose entries are expressions, each kept whole in its parentheses —
     * {@code (cos(theta)) * x + (-sin(theta)) * y + (0)} — the translation last.
     */
    static String expressionRow(List<String> entries, String translation, List<String> variables) {
        List<String> terms = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            terms.add("(" + entries.get(index) + ") * " + variables.get(index));
        }
        terms.add("(" + translation + ")");
        return String.join(" + ", terms);
    }

    private static String sign(double value) {
        return value < 0 ? "-" : "+";
    }

    /**
     * The coefficients of a written row, one per variable, and its translation last.
     *
     * @throws IllegalArgumentException when the row is not a sum of numbers times the given variables
     */
    static double[] read(String written, List<String> variables) {
        Formula row;
        try {
            row = FormulaLanguage.standard().parse(written);
        } catch (FormulaException unreadable) {
            throw new IllegalArgumentException("'" + written + "' is not an affine row such as '0.85 x + 0.04 y + 1.6': "
                    + unreadable.getMessage(), unreadable);
        }
        for (String name : row.variables()) {
            if (!variables.contains(name)) {
                throw new IllegalArgumentException("'" + name + "' is not a variable of '" + written + "'.");
            }
        }
        AffineForm affine = AffineForm.of(row, variables).orElseThrow(() -> new IllegalArgumentException(
                "'" + written + "' is not an affine row such as '0.85 x + 0.04 y + 1.6'."));
        double[] coefficients = new double[variables.size() + 1];
        for (int index = 0; index < variables.size(); index++) {
            coefficients[index] = affine.coefficients().get(index);
        }
        coefficients[variables.size()] = affine.translation();
        return coefficients;
    }
}
