package org.jmouse.dom.template.build;

import org.jmouse.dom.template.NodeTemplate;
import org.jmouse.dom.template.ValueExpression;

public final class Include {

    private Include() {}

    public static NodeTemplate template(String keyConstant, ValueExpression model) {
        return new NodeTemplate.Include(ValueExpression.constant(keyConstant), model);
    }

    public static NodeTemplate template(ValueExpression keyExpression, ValueExpression model) {
        return new NodeTemplate.Include(keyExpression, model);
    }
}
