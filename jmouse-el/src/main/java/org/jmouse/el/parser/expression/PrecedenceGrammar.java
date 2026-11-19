package org.jmouse.el.parser.expression;

/** Language-owned operands and reductions; the engine owns only precedence ordering. */
public interface PrecedenceGrammar<I, N, O> {
    N primary(I input, ExpressionReader<N> expressions);

    /** Languages may restrict operand forms at the current binding power without replacing the climbing engine. */
    default N primary(I input, int minimum, ExpressionReader<N> expressions) {
        return primary(input, expressions);
    }

    /** Null means that the current input does not open an infix operator. */
    InfixBinding<O> infix(I input, N left);

    /** Consumes the operator and reads its operands through the supplied reader. */
    N reduce(I input, N left, InfixBinding<O> binding, ExpressionReader<N> expressions);

    long position(I input);
}
