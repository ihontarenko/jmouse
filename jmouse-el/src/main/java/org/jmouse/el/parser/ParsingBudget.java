package org.jmouse.el.parser;

/** Explicit per-session limits; neither the source nor a registered rule may change them. */
public record ParsingBudget(int maximumDepth, long maximumSteps) {

    public ParsingBudget {
        if (maximumDepth <= 0 || maximumSteps <= 0) {
            throw new IllegalArgumentException("Parsing limits must be positive");
        }
    }
}
