package org.jmouse.el.parser.expression;

import org.jmouse.el.parser.ParseException;
import org.jmouse.el.parser.ParseSession;

import java.util.Objects;

/**
 * Shared precedence climbing for language-specific nodes. It does not evaluate operands,
 * assume token identities or introduce arithmetic laws such as associativity of floating point.
 */
public final class PrecedenceParser {
    private PrecedenceParser() {}

    public static <I, N, O> N parse(I input, int minimum, PrecedenceGrammar<I, N, O> grammar) {
        return parse(input, minimum, grammar, null);
    }

    public static <I, N, O> N parse(I input, int minimum, PrecedenceGrammar<I, N, O> grammar,
                                   ParseSession session) {
        if (session != null) {
            return session.scoped("expression", () -> read(input, minimum, grammar, session));
        }
        return read(input, minimum, grammar, null);
    }

    private static <I, N, O> N read(I input, int minimum, PrecedenceGrammar<I, N, O> grammar,
                                    ParseSession session) {
        Objects.requireNonNull(grammar, "grammar");
        ExpressionReader<N> expressions = precedence -> parse(input, precedence, grammar, session);
        long start = grammar.position(input);
        N left = Objects.requireNonNull(grammar.primary(input, minimum, expressions), "primary expression");
        if (grammar.position(input) <= start) {
            throw new ParseException("A primary expression rule did not consume input");
        }
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
