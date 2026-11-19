package org.jmouse.java.parser;

import org.jmouse.el.node.NodeLifecycle;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.parser.grammar.JavaParsingMode;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.JavaCoverageLevel;
import org.jmouse.java.parser.source.JavaOmittedRegion;
import org.jmouse.java.parser.source.JavaSourceCoverage;
import org.jmouse.java.parser.source.ParseProblem;

import java.util.List;
import java.util.Objects;

/** A frozen syntax tree, source provenance and all damage reported while reading it. */
public record JavaSyntaxResult<N extends JavaSyntaxNode>(
        N root,
        JavaLexicalSnapshot source,
        JavaSourceProfile profile,
        List<ParseProblem> problems,
        JavaParsingMode mode,
        List<JavaOmittedRegion> omittedRegions) {
    public JavaSyntaxResult(
            N root,
            JavaLexicalSnapshot source,
            JavaSourceProfile profile,
            List<ParseProblem> problems) {
        this(root, source, profile, problems, JavaParsingMode.COMPLETE, List.of());
    }

    public JavaSyntaxResult(N root, JavaLexicalSnapshot source, JavaSourceProfile profile) {
        this(root, source, profile, source.problems());
    }

    public JavaSyntaxResult {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(profile, "profile");
        problems = List.copyOf(problems);
        Objects.requireNonNull(mode, "mode");
        omittedRegions = List.copyOf(omittedRegions);
        if (mode == JavaParsingMode.COMPLETE && !omittedRegions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Complete syntax cannot contain deliberately omitted regions");
        }
        if (root.lifecycle() != NodeLifecycle.FROZEN
                || root.getParent() != null
                || !root.sourceName().equals(source.name())
                || root.span().end().offset() > source.text().length()
                || !problems.containsAll(source.problems())) {
            throw new IllegalArgumentException(
                    "A syntax result requires a frozen source root and all lexical diagnostics");
        }
    }

    public boolean isComplete() {
        return problems.isEmpty() && omittedRegions.isEmpty();
    }

    public void requireComplete() {
        if (mode != JavaParsingMode.COMPLETE || !isComplete()) {
            throw new IllegalStateException(
                    "This operation requires complete syntax without diagnostics or omitted"
                            + " regions");
        }
    }

    public JavaSourceCoverage coverage() {
        return new JavaSourceCoverage(
                mode == JavaParsingMode.COMPLETE
                        ? JavaCoverageLevel.FULL_SYNTAX
                        : JavaCoverageLevel.DECLARATIONS,
                omittedRegions);
    }
}
