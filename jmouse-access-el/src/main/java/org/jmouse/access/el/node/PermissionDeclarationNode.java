package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyPermissionDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

/**
 * One line of a {@code permissions} block: {@code form:read "Read forms"}.
 *
 * <p>The description is for a person, not the engine — it is what a control room shows beside a
 * permission somebody is trying to understand.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class PermissionDeclarationNode extends AbstractExpression {

    private String name;
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
     * @return the name, description, and where it was written
     */
    public PolicyPermissionDeclaration toPermissionDeclaration() {
        return new PolicyPermissionDeclaration(getName(), getDescription(), SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toPermissionDeclaration();
    }

    /**
     * ⚠️ <strong>A missing description is written as an empty literal, never left out.</strong> The
     * parser requires the string, so a line with nothing after the name does not parse — and rendering
     * the null itself put the word {@code null} in the file as if somebody had described it that way.
     * Only a document the parser built can be sure of having one; a projection assembled from a
     * catalogue cannot, because a permission is allowed to go undescribed.
     */
    @Override
    public String toSource() {
        String description = getDescription();

        return "%s %s".formatted(getName(), SourceWriter.literal(description == null ? "" : description));
    }

    @Override
    public String toString() {
        return toSource();
    }
}
