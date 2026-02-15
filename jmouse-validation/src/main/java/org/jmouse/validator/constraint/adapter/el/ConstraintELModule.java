package org.jmouse.validator.constraint.adapter.el;

import org.jmouse.core.mapping.Mapper;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

public final class ConstraintELModule {

    private ConstraintELModule() {}

    /**
     * CODEBASE wiring:
     * - registers EL parser once
     * - creates support facade
     */
    public static ConstraintExpressionSupport create(ExpressionLanguage expressionLanguage, ConstraintTypeRegistry registry, Mapper mapper) {
        expressionLanguage.getExtensions().addParser(new ValidatorDefinitionParser());
        ConstraintExpressionAdapter adapter = new ConstraintExpressionAdapter(registry, mapper);
        return new ConstraintExpressionSupport(expressionLanguage, adapter);
    }
}
