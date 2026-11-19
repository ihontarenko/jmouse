package org.jmouse.java.analysis;

import org.jmouse.java.analysis.jdk.JdkSemanticProvider;

import java.util.Objects;

public final class JavaSemanticAnalyzer {
    private final JavaSemanticProvider provider;

    public JavaSemanticAnalyzer() {
        this(new JdkSemanticProvider());
    }

    public JavaSemanticAnalyzer(JavaSemanticProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public JavaAnalysisResult analyze(JavaAnalysisRequest request) {
        return provider.analyze(Objects.requireNonNull(request, "request"));
    }
}
