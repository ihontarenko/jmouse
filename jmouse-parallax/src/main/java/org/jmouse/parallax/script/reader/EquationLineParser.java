package org.jmouse.parallax.script.reader;

import org.jmouse.el.lexer.BasicToken;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.type.ValueType;
import org.jmouse.parallax.script.Block.Equation;
import org.jmouse.parallax.script.Block.EquationForm;
import org.jmouse.parallax.script.SourceText;

import java.util.Set;

/**
 * {@code d(x)/dt = …}, {@code x_next = …}, {@code x_start = …}, {@code current a -> b = …} (a current from pin a to pin
 * b through the part), {@code voltage a -> b = …} (a branch holding pin a at that voltage above pin b), {@code 0 = …},
 * {@code let name = …} or {@code let name : current = …} ({@code local} is the same word). Every right side is an
 * expression jMF's grammar reads, to the end of the line.
 */
final class EquationLineParser extends JmxLineParser<Equation> {

    private static final String SHAPE = "an equation is d(x)/dt = …, x_next = …, x_start = …, current a -> b = …, "
            + "voltage a -> b = …, 0 = … or let name [: type] = …";
    private static final String PIN = "[A-Za-z_][A-Za-z0-9_]*";
    private static final String NEXT = "_next";
    private static final String START = "_start";

    private final Reading reading;
    private final JmxValues values;
    private final DeclarationReader declarations;

    /**
     * @param declarations the types the document has named so far, which a {@code let} may be typed by
     */
    EquationLineParser(Reading reading, JmxValues values, DeclarationReader declarations) {
        this.reading = reading;
        this.values = values;
        this.declarations = declarations;
    }

    @Override
    Equation read() {
        TokenCursor line = reading.lineView();
        Token first = reading.cursor.current();
        String head = line.valueAt(0);
        if (head.equals("let") || head.equals("local")) {
            if (!line.checkValueAt(2, ":")) {
                reading.requireEqualsAhead(2);
                String name = line.valueAt(1);
                return new Equation(EquationForm.LOCAL, name, null, null, rightSide(3));
            }
            String typeName = line.valueAt(3);
            ValueType type = declarations.resolveType(typeName).orElseThrow(() -> reading.refusalAhead(3, "'" + typeName
                    + "' is not a type here. Known types: " + String.join(", ", declarations.knownTypeNames())));
            reading.requireEqualsAhead(4);
            String name = line.valueAt(1);
            return new Equation(EquationForm.LOCAL, name, null, null, rightSide(5), type);
        }
        if (head.equals("d") && line.checkValueAt(1, "(") && line.checkValueAt(3, ")") && line.checkValueAt(4, "/")) {
            if (!line.checkValueAt(5, "dt")) {
                throw reading.refusalAhead(5, "a derivative is written d(x)/dt = …");
            }
            reading.requireEqualsAhead(6);
            String name = line.valueAt(2);
            return new Equation(EquationForm.DERIVATIVE, name, null, null, rightSide(7));
        }
        if (head.equals("current") || head.equals("voltage")) {
            int equals = line.offsetOf(1, "=");
            String[] pins = equals < 0 ? new String[0] : joined(line, 1, equals).split("->", -1);
            if (pins.length != 2 || !pins[0].matches(PIN) || !pins[1].matches(PIN)) {
                throw reading.refusalAhead(1, "a branch is written '" + head + " a -> b = …', from one pin to another");
            }
            reading.requireEqualsAhead(equals);
            return new Equation(head.equals("current") ? EquationForm.CURRENT : EquationForm.VOLTAGE, null, pins[0],
                    pins[1], rightSide(equals + 1));
        }
        if ((head.equals("I") || head.equals("V")) && line.checkValueAt(1, "(")) {
            throw reading.refusal(first, "a branch is written '" + (head.equals("I") ? "current" : "voltage")
                    + " a -> b = …', not " + head + "(a, b) = …");
        }
        if (head.equals("0") && line.checkValueAt(1, "=")) {
            return new Equation(EquationForm.IMPLICIT, null, null, null, rightSide(2));
        }
        if (line.checkValueAt(1, "=") && first.type() == BasicToken.T_IDENTIFIER) {
            if (head.endsWith(NEXT) && head.length() > NEXT.length()) {
                return new Equation(EquationForm.NEXT, head.substring(0, head.length() - NEXT.length()), null, null,
                        rightSide(2));
            }
            if (head.endsWith(START) && head.length() > START.length()) {
                return new Equation(EquationForm.START, head.substring(0, head.length() - START.length()), null, null,
                        rightSide(2));
            }
        }
        throw reading.refusal(first, SHAPE);
    }

    /** {@code temperature = …} of a heat law: whether the line says what the temperature is. */
    boolean atTemperature() {
        TokenCursor line = reading.lineView();
        return line.checkValueAt(0, "temperature") && line.checkValueAt(1, "=");
    }

    /** The right side of {@code temperature = …}. */
    SourceText temperature() {
        reading.requireEqualsAhead(1);
        return rightSide(2);
    }

    /** The expression from token {@code at} of the line to its end. */
    private SourceText rightSide(int at) {
        reading.cursor.shift(at);
        SourceText expression = values.expression(Set.of());
        reading.requireLineEnd();
        return expression;
    }

    /** The words of tokens {@code [from, to)} of the line, written together. */
    private static String joined(TokenCursor line, int from, int to) {
        StringBuilder words = new StringBuilder();
        for (int offset = from; offset < to; offset++) {
            words.append(line.valueAt(offset));
        }
        return words.toString();
    }
}
