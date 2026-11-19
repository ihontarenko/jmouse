package org.jmouse.java.parser.node.expression;

import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaNodeKindNames;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.Objects;

/** Damaged source preserved by recovery; this node never supplies an invented Java value. */
public final class JavaErrorExpressionNode extends JavaExpressionNode {
    public static final NodeKind<JavaErrorExpressionNode> KIND =
            new NodeKind<>(JavaNodeKindNames.ERROR_EXPRESSION, JavaErrorExpressionNode.class);
    public static final NodeSchema<JavaErrorExpressionNode> SCHEMA = NodeSchema.of(KIND);
    private final ParseProblem problem;

    public JavaErrorExpressionNode(String sourceName, SourceSpan span, ParseProblem problem) {
        super(SCHEMA, sourceName, span);
        this.problem = Objects.requireNonNull(problem, "problem");
    }

    public ParseProblem problem() {
        return problem;
    }
}
