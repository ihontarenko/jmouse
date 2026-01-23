package org.jmouse.core.mapping.errors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects mapping problems and supports fail-fast or collect-and-throw modes.
 */
public final class MappingDiagnostics {

    private final List<MappingProblem> problems = new ArrayList<>();

    public void add(MappingProblem problem) {
        problems.add(problem);
    }

    public boolean hasErrors() {
        return problems.stream().anyMatch(problem -> problem.severity() == MappingProblem.Severity.ERROR);
    }

    public List<MappingProblem> problems() {
        return Collections.unmodifiableList(problems);
    }
}
