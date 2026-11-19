package org.jmouse.java.parser.node.statement;

import org.jmouse.java.parser.node.JavaChildRoleNames;

import org.jmouse.java.parser.node.JavaNodeKindNames;

import java.util.List;
import org.jmouse.el.node.ChildRole;
import org.jmouse.el.node.NodeKind;
import org.jmouse.el.node.NodeSchema;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.source.SourceSpan;

/** Typed switch-rule syntax, with role-owned children in source order. */
public final class JavaSwitchRuleNode extends JavaSyntaxNode {
    public static final NodeKind<JavaSwitchRuleNode> KIND = new NodeKind<>(JavaNodeKindNames.SWITCH_RULE, JavaSwitchRuleNode.class);
    public static final ChildRole<JavaCaseLabelNode> LABELS = ChildRole.list(JavaChildRoleNames.LABELS, JavaCaseLabelNode.class);
    public static final ChildRole<JavaSyntaxNode> OUTCOME = ChildRole.required(JavaChildRoleNames.OUTCOME, JavaSyntaxNode.class);
    public static final NodeSchema<JavaSwitchRuleNode> SCHEMA = NodeSchema.of(KIND, LABELS, OUTCOME);

    public JavaSwitchRuleNode(String sourceName, SourceSpan span) {
        super(SCHEMA, sourceName, span);
    }

    public List<JavaCaseLabelNode> labels() {
        return children(LABELS);
    }

    public void addLabel(JavaCaseLabelNode node) {
        addChild(LABELS, node);
    }

    public JavaSyntaxNode outcome() {
        return child(OUTCOME);
    }

    public void setOutcome(JavaSyntaxNode node) {
        setChild(OUTCOME, node);
    }
}

