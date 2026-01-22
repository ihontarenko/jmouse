package org.jmouse.core.mapping.plan.support;

import org.jmouse.core.mapping.MappingException;
import org.jmouse.core.mapping.diagnostics.MappingProblem;

import java.util.List;

import static org.jmouse.core.mapping.diagnostics.MappingProblem.Severity;

public final class MappingFailures {

    private MappingFailures() {}

    public static MappingException fail(String code, String message, Throwable cause) {
        MappingProblem.Builder builder = MappingProblem.builder();

        builder.severity(Severity.ERROR)
                .code(code).message(message).cause(cause);

        MappingProblem problem = builder.build();

        if (cause == null) {
            return new MappingException(message, List.of(problem));
        }

        return new MappingException(message, cause, List.of(problem));
    }
}
