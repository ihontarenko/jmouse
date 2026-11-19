package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.Optional;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.pattern.JavaPatternNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed pattern-case-label syntax, with role-owned children in source order. */
public final class JavaPatternCaseLabelNode extends JavaCaseLabelNode {
    public static final NodeKind<JavaPatternCaseLabelNode> KIND = new NodeKind<>(JavaNodeKindNames.PATTERN_CASE_LABEL, JavaPatternCaseLabelNode.class);
    public static final ChildRole<JavaPatternNode> PATTERN = ChildRole.required(JavaChildRoleNames.PATTERN, JavaPatternNode.class);
    public static final ChildRole<JavaCaseGuardNode> GUARD = ChildRole.optional(JavaChildRoleNames.GUARD, JavaCaseGuardNode.class);
    public static final NodeSchema<JavaPatternCaseLabelNode> SCHEMA = NodeSchema.of(KIND, PATTERN, GUARD);

    public JavaPatternCaseLabelNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public JavaPatternNode pattern() {
        return child(PATTERN);
    }

    public void setPattern(JavaPatternNode node) {
        setChild(PATTERN, node);
    }

    public Optional<JavaCaseGuardNode> guard() {
        return optionalChild(GUARD);
    }

    public void setGuard(JavaCaseGuardNode node) {
        setChild(GUARD, node);
    }
}

