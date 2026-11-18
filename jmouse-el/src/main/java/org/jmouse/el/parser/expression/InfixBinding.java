package org.jmouse.el.parser.expression;

import java.util.Objects;

public record InfixBinding<O>(O operator, int precedence, Associativity associativity) {
    public InfixBinding {
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(associativity, "associativity");
        if (precedence < 0 || precedence == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid operator precedence: " + precedence);
        }
    }

    public int rightMinimum() {
        return associativity == Associativity.RIGHT ? precedence : precedence + 1;
    }
}
