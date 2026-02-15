package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.expression.KeyValueNode;
import org.jmouse.validator.constraint.model.ValidationDefinition;

import java.util.Map;

public class ValidationDefinitionNode extends AbstractExpression {

    private final String name;

    public ValidationDefinitionNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        ValidationDefinition definition = new ValidationDefinition(getName());

        for (Node child : getChildren()) {
            if (child instanceof KeyValueNode keyValue) {
                if (keyValue.evaluate(context) instanceof Map.Entry<?, ?> entry) {
                    definition.put((String) entry.getKey(), entry.getValue());
                }
            }
        }

        return definition;
    }

}
