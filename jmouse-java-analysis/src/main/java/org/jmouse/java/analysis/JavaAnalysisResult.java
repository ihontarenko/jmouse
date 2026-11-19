package org.jmouse.java.analysis;

import java.util.List;
import java.util.Objects;

public record JavaAnalysisResult(
        JavaAnalyzedProgram program, List<JavaSemanticDiagnostic> diagnostics) {
    public JavaAnalysisResult {
        Objects.requireNonNull(program, "program");
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean isSuccessful() {
        return program.coverage() == JavaSemanticCoverage.COMPLETE
                && diagnostics.stream()
                        .noneMatch(
                                diagnostic ->
                                        diagnostic.severity() == JavaDiagnosticSeverity.ERROR);
    }

    public JavaAnalyzedProgram requireSuccessful() {
        if (!isSuccessful()) {
            throw new IllegalStateException(
                    "Semantic analysis did not produce a complete valid program: " + diagnostics);
        }
        return program;
    }
}
