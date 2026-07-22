package org.jmouse.el.node.expression;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;

public class VariableAliasNode extends AbstractExpression {

    private String aliasName;
    private String variableName;

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        context.setValue(
                getAliasName(), context.getValue(
                        getVariableName()
                )
        );
        return null;
    }

    @Override
    public String toString() {
        return aliasName + " <- " + variableName;
    }
}
