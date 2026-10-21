package org.jmouse.parallax.script.engine;

import org.jmouse.formula.AffineForm;
import org.jmouse.formula.Formula;
import org.jmouse.formula.FormulaException;
import org.jmouse.formula.FormulaPrinter;
import org.jmouse.formula.el.FormulaLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * One row of an affine map written out as the formula it is. With numeric coefficients (an IFS map):
 * {@code -0.04 * x + 0.85 * y + 1.6} — built as a jMF tree and printed by jMF, a zero term left out, a coefficient of one
 * not written, a row with no terms at all {@code 0}. Reading is jMF's too: the row is parsed as a formula and its
 * coefficients are taken from the tree ({@link AffineForm}), so every coefficient written reads back as the same
 * {@code double}.
 */
final class AffineRows {

    private AffineRows() {
    }

    /** {@code coefficients · variables + translation}, as the sum jMF prints. */
    static String write(List<Double> coefficients, double translation, List<String> variables) {
        Formula row = null;
        for (int index = 0; index < coefficients.size(); index++) {
            row = withTerm(row, coefficients.get(index), new Formula.Variable(variables.get(index)));
        }
        row = withTerm(row, translation, null);
        return FormulaPrinter.text(row == null ? new Formula.Constant(0) : row);
    }

    /**
     * The sum so far with one more term: {@code coefficient · variable}, or the coefficient alone for the translation
     * (no variable). A zero term adds nothing; the first term carries its own sign, every later one is added or taken away.
     */
    private static Formula withTerm(Formula sum, double coefficient, Formula variable) {
        if (!Double.isFinite(coefficient)) {
            throw new IllegalArgumentException("An affine row's coefficients are finite, not " + coefficient + ".");
        }
        if (coefficient == 0) {
            return sum;
        }
        double magnitude = Math.abs(coefficient);
        if (sum == null) {
            return term(coefficient, variable);
        }
        Formula.Operator operator = coefficient < 0 ? Formula.Operator.SUBTRACT : Formula.Operator.ADD;
        return new Formula.Binary(operator, sum, term(magnitude, variable));
    }

    /** {@code 0.85 * x}, {@code x} for a coefficient of one, {@code -x} for minus one, the number alone without a variable. */
    private static Formula term(double coefficient, Formula variable) {
        if (variable == null) {
            return new Formula.Constant(coefficient);
        }
        if (Math.abs(coefficient) == 1) {
            return coefficient < 0 ? new Formula.Negation(variable) : variable;
        }
        return new Formula.Binary(Formula.Operator.MULTIPLY, new Formula.Constant(coefficient), variable);
    }

    /**
     * One row of an affine map whose entries are expressions, each kept whole in its parentheses —
     * {@code (cos(theta)) * x + (-sin(theta)) * y + (0)} — the translation last.
     *
     * <p>Not built as a jMF tree, deliberately: the entries are the author's expressions, kept as written, and this
     * shape is the grammar the reader cuts them back out by. Printing a parsed entry would rewrite the author's text
     * ({@code sin(r)/r} as {@code sin(r) / r}), so a document would no longer read back to the definition it was written
     * from.</p>
     */
    static String expressionRow(List<String> entries, String translation, List<String> variables) {
        List<String> terms = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            terms.add("(" + entries.get(index) + ") * " + variables.get(index));
        }
        terms.add("(" + translation + ")");
        return String.join(" + ", terms);
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
