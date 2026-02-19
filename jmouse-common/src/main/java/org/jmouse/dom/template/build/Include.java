package org.jmouse.dom.template.build;

import org.jmouse.dom.template.NodeTemplate;
import org.jmouse.dom.template.ValueExpression;

import static org.jmouse.dom.template.build.Blueprints.*;

public final class Include {

    private Include() {}

    public static NodeTemplate blueprint(String keyConstant, ValueExpression model) {
        return new NodeTemplate.Include(constant(keyConstant), model);
    }

    public static NodeTemplate blueprint(ValueExpression keyExpression, ValueExpression model) {
        return new NodeTemplate.Include(keyExpression, model);
    }
}
