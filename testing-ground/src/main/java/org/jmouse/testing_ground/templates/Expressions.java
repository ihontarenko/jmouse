package org.jmouse.testing_ground.templates;

import org.jmouse.core.bind.PropertyPath;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.MethodImporter;
import org.jmouse.el.extension.calculator.MathematicCalculator;
import org.jmouse.el.extension.i18nExtension;
import org.jmouse.testing_ground.binder.dto.Status;
import org.jmouse.testing_ground.binder.dto.User;
import org.jmouse.testing_ground.binder.dto.UserStatus;
import org.jmouse.util.helper.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Expressions {

    public static void main(String[] args) {
        ExpressionLanguage el = new ExpressionLanguage();

        PropertyPath path = PropertyPath.forPath("user.names[0].default");

        for (CharSequence entry : path.entries()) {
            System.out.println(entry);
        }

        EvaluationContext  context = el.newContext();

//        el.getExtensions().importExtension(new i18nExtension());

//        MathematicCalculator.PLUS.calculate(1, 2);

        Function<String, Integer> toInt  = Integer::parseInt;
        Function<Integer, Long>   toLong = Integer::longValue;

        BigDecimal decimal = toInt.andThen(toLong.andThen(BigDecimal::new)).apply("123");

        MethodImporter.importMethod(Strings.class, context.getExtensions());

        context.setValue("test", 256);
        context.setValue("time", System.currentTimeMillis());

        User user = new User();
        user.setName("Ivan");
        user.setStatus(new UserStatus(Status.REGISTERED));

        User user2 = new User();
        user2.setName("Borys");
        user2.setStatus(new UserStatus(Status.BLOCKED));

        context.setValue("user", user);

//        System.out.println(
//                el.evaluate("12 + 3.14")
//        );

//        System.out.println(
//                el.evaluate("12 + 1123456789123456.78")
//        );
//
//        System.out.println(
//                el.evaluate("1.2 + 314")
//        );

        context.setValue("list", new ArrayList<>());

        context.setValue("ci", (char) 23);
        context.setValue("data", List.of(1, 2, 3));
        context.setValue("strings", List.of("ZZ", "YY"));

        el.compile("[1, 2, 3, 4, 5]|map(i -> i * 2)").evaluate(context);

        el.compile("1 + (2 * 2) | int > 5 and 4 < 5 - 2 / 3");
        el.evaluate("22 / 7");
        el.evaluate("2 .. 22 / 2");
        el.evaluate("'a' .. strings[0]", context);
        el.evaluate("time - 2", context);

        Double d2 = el.evaluate("22f / 7", Double.class);

//        el.evaluate("i18n('i18n.default', 'jmouse.el.name', 1, 2, 3) | upper");

        el.evaluate("set('tag', (name, value) -> '<' ~ name ~ '>' ~ value ~ '</' ~ name ~ '>')", context);
        el.evaluate("tag('abc', 'qwe')", context);
        el.evaluate("() -> 'hello'");
        el.evaluate("() -> {}");
        el.evaluate("set('toString', (v) -> v|string)", context);
        el.evaluate("set('getNumberType', (v) -> v|int is even ? 'Even' : 'Odd')", context);
        el.evaluate("getNumberType(1i)", context);

        el.evaluate("toString(123.123)");

        el.evaluate("list + (((list | length is even) ? 'Even' : 'Odd') | upper)", context);
        el.evaluate("list + ((ci + 2 + (14 - 1) | int / 7) is even)", context);

        el.evaluate("user.name ?? 'Guest'", context);
        el.evaluate("list + (list is type('collection')) | string", context);
        el.evaluate("123 is type('collection')", context);
        el.evaluate("[1, 2, 3]");
        el.evaluate("{user.status.status | string : user.name | length | float}", context);

        el.evaluate("set('var', cut(user.name | upper, '_', false, false, 1|int))", context);

        Object value = el.evaluate("(lclast(var) | last) ~ '22'", context);

        el.evaluate("set('pi', 22 / 7)", context);
        el.evaluate("set('username', user.name)", context);

        EvaluationContext ctx = el.newContext();
        ctx.setValue("user", user2);
        el.evaluate("set('username', user.name)", ctx);

        el.evaluate("set('result', isEmpty(''))", context);
        el.evaluate("set(user.status.status, isEmpty(''))", context);
        el.evaluate("set(user.status.status, isEmpty(''))", ctx);

        System.out.println(value);

        System.out.println("ctx1: " + context.getValue("username"));
        System.out.println("ctx2: " + ctx.getValue("username"));

        el.evaluate("set('cnt', 0)", context);

        el.evaluate("3.14 * 7");
        Double d1 = el.evaluate("22 | double / 7", Double.class);

        context.setValue("x", new BigInteger("23"));
        context.setValue("name", "Ivan");

        el.evaluate("x * 7", context);
        el.evaluate("name * 3", context);
        el.evaluate("name + 3", context);

        List<String> names = new ArrayList<>();

        context.setValue("names", names);
        context.setValue("si", (short) 23);
        context.setValue("bi", (byte) 23);

        el.evaluate("si + 2", context);
        el.evaluate("(bi + 2) | int", context);
        el.evaluate("bi + 2", context);
        el.evaluate("ci + 2", context);

        el.evaluate("names + user.name", context);
        el.evaluate("names + 'John'", context);
        el.evaluate("names + 'Doe'", context);

        long start = System.currentTimeMillis();



        long spend = 0;
        int  times = 0;

        while (spend < 1000) {
            times++;
            spend = System.currentTimeMillis() - start;
            el.evaluate("user.name ~ '22' | upper", context);
            el.evaluate("cnt++", context);
            el.evaluate("names + cnt", context);
//            compiled.evaluate(evaluationContext);
        }

        System.out.println("times: " + times);
    }

}
