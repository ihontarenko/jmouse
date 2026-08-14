package org.jmouse.access.el.node;

import org.jmouse.access.VariableKind;
import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyVariableDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One line of a {@code variables} block: {@code constant deployment "Which deployment this is"}.
 *
 * <p>The kind word is the statement. {@code constant} says the value is settled before any call
 * arrives, {@code dynamic} says it is worked out from the call being decided — and the two are exactly
 * what a publisher chose between when it attached the value outright or deferred it, so the file can
 * be held to it.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class VariableDeclarationNode extends AbstractExpression {

    private String       name;
    private VariableKind kind = VariableKind.CONSTANT;
    private String       description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VariableKind getKind() {
        return kind;
    }

    public void setKind(VariableKind kind) {
        this.kind = kind == null ? VariableKind.CONSTANT : kind;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns this declaration as the record stage 2 receives.
     *
     * @return the name, its kind, its description, and where it was written
     */
    public PolicyVariableDeclaration toVariableDeclaration() {
        return new PolicyVariableDeclaration(
                getName(), getKind(), getDescription(), SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toVariableDeclaration();
    }

    @Override
    public String toSource() {
        StringBuilder source = new StringBuilder(keyword()).append(' ').append(getName());

        if (getDescription() != null) {
            source.append(' ').append(SourceWriter.literal(getDescription()));
        }

        return source.toString();
    }

    /** The word this kind is written with — the one place the two spellings are paired. */
    private String keyword() {
        return getKind() == VariableKind.DYNAMIC ? "dynamic" : "constant";
    }

    @Override
    public String toString() {
        return toSource();
    }
}
