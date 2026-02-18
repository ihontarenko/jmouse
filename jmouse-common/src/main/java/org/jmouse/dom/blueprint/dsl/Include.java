package org.jmouse.dom.blueprint.dsl;

import org.jmouse.dom.blueprint.Blueprint;
import org.jmouse.dom.blueprint.BlueprintValue;

import static org.jmouse.dom.blueprint.dsl.Blueprints.*;

public final class Include {

    private Include() {}

    public static Blueprint blueprint(String keyConstant, BlueprintValue model) {
        return new Blueprint.IncludeBlueprint(constant(keyConstant), model);
    }

    public static Blueprint blueprint(BlueprintValue keyExpression, BlueprintValue model) {
        return new Blueprint.IncludeBlueprint(keyExpression, model);
    }
}
