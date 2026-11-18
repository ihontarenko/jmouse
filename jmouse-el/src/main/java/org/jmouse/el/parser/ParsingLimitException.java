package org.jmouse.el.parser;

/** A controlled input refusal, not a JVM stack or memory failure. */
public final class ParsingLimitException extends ParseException {
    public ParsingLimitException(String message) {
        super(message);
    }
}
