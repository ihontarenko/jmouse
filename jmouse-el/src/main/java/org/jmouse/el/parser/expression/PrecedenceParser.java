package org.jmouse.el.parser.expression;

import org.jmouse.el.parser.ParseException;

import java.util.Objects;

/**
 * Shared precedence climbing for language-specific nodes. It does not evaluate operands,
 * assume token identities or introduce arithmetic laws such as associativity of floating point.
 */
public final class PrecedenceParser {
    private PrecedenceParser() {}

    public static <I, N, O> N parse(I input, int minimum, PrecedenceGrammar<I, N, O> grammar) {
        Objects.requireNonNull(grammar, "grammar");
        ExpressionReader<N> expressions = precedence -> parse(input, precedence, grammar);
        N left = Objects.requireNonNull(grammar.primary(input, expressions), "primary expression");
        while (true) {
            InfixBinding<O> binding = grammar.infix(input, left);
            if (binding == null || binding.precedence() < minimum) {
                return left;
            }
            long before = grammar.position(input);
            left = Objects.requireNonNull(grammar.reduce(input, left, binding, expressions), "reduced expression");
            if (grammar.position(input) <= before) {
                throw new ParseException("An expression rule did not consume input: " + binding.operator());
            }
        }
    }
}
