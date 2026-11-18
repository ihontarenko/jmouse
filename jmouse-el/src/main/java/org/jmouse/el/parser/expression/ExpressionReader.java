package org.jmouse.el.parser.expression;

/** Re-enters the same grammar with an explicit binding threshold. */
@FunctionalInterface
public interface ExpressionReader<N> {
    N parse(int minimumPrecedence);
}
