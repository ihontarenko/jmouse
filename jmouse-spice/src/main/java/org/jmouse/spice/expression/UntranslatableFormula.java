package org.jmouse.spice.expression;

/** A jMF formula SPICE has no way to say, and why: a derivative, a host call, an infinite value. */
public final class UntranslatableFormula extends RuntimeException {

    public UntranslatableFormula(String message) {
        super(message);
    }
}
