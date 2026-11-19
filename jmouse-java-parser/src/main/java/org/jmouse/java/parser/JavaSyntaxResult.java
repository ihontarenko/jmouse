package org.jmouse.java.parser;

import java.util.Objects;
import java.util.List;
import org.jmouse.el.node.NodeLifecycle;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.ParseProblem;

/** A frozen syntax tree, source provenance and all damage reported while reading it. */
public record JavaSyntaxResult<N extends JavaSyntaxNode>(N root, JavaLexicalSnapshot source, JavaSourceProfile profile,
                                                       List<ParseProblem> problems) {
    public JavaSyntaxResult(N root, JavaLexicalSnapshot source, JavaSourceProfile profile) {
        this(root, source, profile, source.problems());
    }

    public JavaSyntaxResult {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(profile, "profile");
        problems = List.copyOf(problems);
        if (root.lifecycle() != NodeLifecycle.FROZEN || root.getParent() != null
                || !root.sourceName().equals(source.name()) || root.span().end().offset() > source.text().length()
                || !problems.containsAll(source.problems())) {
            throw new IllegalArgumentException("A syntax result requires a frozen source root and all lexical diagnostics");
        }
    }

    public boolean isComplete() {
        return problems.isEmpty();
    }
}
