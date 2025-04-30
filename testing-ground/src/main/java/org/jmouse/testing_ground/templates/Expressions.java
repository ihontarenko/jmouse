package org.jmouse.testing_ground.templates;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.MethodImporter;
import org.jmouse.testing_ground.binder.dto.Status;
import org.jmouse.testing_ground.binder.dto.User;
import org.jmouse.testing_ground.binder.dto.UserStatus;
import org.jmouse.util.helper.Strings;

public class Expressions {

    public static void main(String[] args) {
        ExpressionLanguage el = new ExpressionLanguage();

        EvaluationContext  evaluationContext = el.newContext();

        MethodImporter.importMethod(Strings.class, evaluationContext.getExtensions());

        evaluationContext.setValue("test", 256);

        User user = new User();
        user.setName("IvanHontarenkoBorys");
        user.setStatus(new UserStatus(Status.REGISTERED));

        evaluationContext.setValue("user", user);

        el.evaluate("set('var', cut(user.name | upper, '_', false, false, 1|int))", evaluationContext);

        Object value = el.evaluate("lclast(var) ~ '22'", evaluationContext);

        el.evaluate("set('math', 22 / 7)", evaluationContext);
        el.evaluate("set('username', user.name)", evaluationContext);
        el.evaluate("set('result', isEmpty(''))", evaluationContext);
        el.evaluate("set(user.name ~ '.' ~ user.status.status, isEmpty(''))", evaluationContext);

        System.out.println(value);

        long start = System.currentTimeMillis();
        long spend = 0;
        int  times = 0;

        while (spend < 1000) {
            times++;
            spend = System.currentTimeMillis() - start;
            el.evaluate("user.name ~ '22' | upper", evaluationContext);
//            compiled.evaluate(evaluationContext);
        }

        System.out.println("times: " + times);
    }

}
