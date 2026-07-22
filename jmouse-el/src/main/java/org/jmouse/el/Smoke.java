package org.jmouse.el;

import org.jmouse.el.evaluation.EvaluationContext;

public class Smoke {

    public static void main(String[] arguments) {
        ExpressionLanguage el = new ExpressionLanguage();
        EvaluationContext context = el.newContext();

        context.setValue("f_textarea-00005-id", 22f);
        context.setValue("V", 130f);

        el.evaluate("$I2:f_textarea-00005-id", context);
        el.evaluate("I2 / V", context);
    }

}
