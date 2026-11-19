package org.jmouse.java.analysis;

/** A provider returns only jMouse values and never mutates the supplied syntax trees. */
public interface JavaSemanticProvider {
    JavaAnalysisResult analyze(JavaAnalysisRequest request);
}
