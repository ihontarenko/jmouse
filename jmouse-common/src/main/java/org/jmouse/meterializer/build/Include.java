package org.jmouse.meterializer.build;

import org.jmouse.meterializer.NodeTemplate;
import org.jmouse.meterializer.ValueExpression;

public final class Include {

    private Include() {}

    public static NodeTemplate template(String keyConstant, ValueExpression model) {
        return new NodeTemplate.Include(ValueExpression.constant(keyConstant), model);
    }

    public static NodeTemplate template(ValueExpression keyExpression, ValueExpression model) {
        return new NodeTemplate.Include(keyExpression, model);
    }
}
