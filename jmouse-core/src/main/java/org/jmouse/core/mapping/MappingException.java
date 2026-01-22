package org.jmouse.core.mapping;

import org.jmouse.core.mapping.diagnostics.MappingProblem;

import java.util.List;

/**
 * Mapping failure with a structured problem list for diagnostics.
 */
public final class MappingException extends RuntimeException {

    private final List<MappingProblem> problems;

    public MappingException(String message, List<MappingProblem> problems) {
        super(message);
        this.problems = problems;
    }

    public MappingException(String message, Throwable cause, List<MappingProblem> problems) {
        super(message, cause);
        this.problems = problems;
    }

    public List<MappingProblem> problems() {
        return problems;
    }
}
