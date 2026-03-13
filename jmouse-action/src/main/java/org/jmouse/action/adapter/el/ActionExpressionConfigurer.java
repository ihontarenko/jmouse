package org.jmouse.action.adapter.el;

import org.jmouse.el.ExpressionLanguage;

/**
 * Registers EL extensions required for action expressions. 🧩
 */
public final class ActionExpressionConfigurer {

    private ActionExpressionConfigurer() {
    }

    /**
     * Registers action-related parsers into the EL engine.
     */
    public static void configure(ExpressionLanguage expressionLanguage) {
        expressionLanguage.getExtensions().addParser(new ActionDefinitionParser());
    }
}