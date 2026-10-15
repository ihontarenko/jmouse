package org.jmouse.formula;

/** A compiled real-valued expression; it reads its symbols from one array laid out by {@link Symbols}. */
@FunctionalInterface
public interface RealFunction {

    double evaluate(double[] values);
}
