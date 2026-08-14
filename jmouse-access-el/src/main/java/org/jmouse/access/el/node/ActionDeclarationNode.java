package org.jmouse.access.el.node;

import org.jmouse.access.el.SourceWriter;
import org.jmouse.access.policy.model.PolicyActionDeclaration;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * One line of an {@code actions} block:
 * {@code entry.listByPurpose "List submissions of one purpose" produces purpose, tier}.
 *
 * <p>The description is for a person — it is what a policy editor shows beside an action somebody is
 * scoping a rule to. The {@code produces} clause is what makes that rule writable: it names the
 * values the action carries, so an editor can offer them and a validator can refuse a rule mentioning
 * one this action does not have.
 *
 * <p>⚠️ <strong>What this call is about, and nothing that is merely true while it runs.</strong> A
 * value attached to every decision is declared once in a {@code variables} block; listed here it
 * would have to be repeated against every action, and every one of those lines would be saying
 * something about a route that is not so.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ActionDeclarationNode extends AbstractExpression {

    private String       name;
    private String       description;
    private List<String> values = new ArrayList<>();

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

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values == null ? new ArrayList<>() : values;
    }

    /**
     * Returns this declaration as the record stage 2 receives.
     *
     * @return the name, description, published values, and where it was written
     */
    public PolicyActionDeclaration toActionDeclaration() {
        return new PolicyActionDeclaration(
                getName(), getDescription(), getValues(), SourceSpanNode.at(this));
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        return toActionDeclaration();
    }

    @Override
    public String toSource() {
        StringBuilder source = new StringBuilder(getName());

        if (getDescription() != null) {
            source.append(' ').append(SourceWriter.literal(getDescription()));
        }

        if (!getValues().isEmpty()) {
            source.append(" produces ").append(String.join(", ", getValues()));
        }

        return source.toString();
    }

    @Override
    public String toString() {
        return toSource();
    }
}
