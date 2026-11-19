package org.jmouse.java.analysis;

import org.jmouse.java.model.symbol.JavaSourceLocation;

import java.util.Objects;
import java.util.Optional;

public record JavaSemanticDiagnostic(
        JavaDiagnosticSeverity severity,
        String code,
        String message,
        Optional<JavaSourceLocation> source) {
    public JavaSemanticDiagnostic {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(source, "source");
    }
}
