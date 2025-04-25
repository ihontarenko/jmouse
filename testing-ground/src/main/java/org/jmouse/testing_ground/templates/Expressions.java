package org.jmouse.testing_ground.templates;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.reflection.MethodFilter;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.el.ExpressionEngine;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.CoreExtension;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.Function;
import org.jmouse.el.extension.MethodImporter;
import org.jmouse.el.extension.function.reflection.JavaReflectedFunction;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.testing_ground.binder.dto.User;
import org.jmouse.util.helper.Strings;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class Expressions {

    public static void main(String[] args) {
        ExpressionEngine engine = new ExpressionEngine();

        EvaluationContext  evaluationContext = engine.newContext();
        MethodImporter.importMethod(Strings.class, evaluationContext.getExtensions());

        evaluationContext.setValue("test", 256);

        User user = new User();
        user.setName("IvanHontarenkoBorys");

        evaluationContext.setValue("user", user);

        engine.evaluate("set('var', cut(user.name | upper, '_', false, false, 1|int))", evaluationContext);

        Object value = engine.evaluate("lclast(var)", evaluationContext);

        engine.evaluate("set('math', 22 / 7)", evaluationContext);

        System.out.println(value);

        long start = System.currentTimeMillis();
        long spend = 0;
        int  times = 0;

        while (spend < 1000) {
            times++;
            spend = System.currentTimeMillis() - start;
            engine.evaluate("cut(user.name | upper, '_', false, false, 1)", evaluationContext);
//            compiled.evaluate(evaluationContext);
        }

        System.out.println("times: " + times);
    }

}
