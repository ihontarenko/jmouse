package org.jmouse.el;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.MethodImporter;
import org.jmouse.el.renderable.Template;
import org.jmouse.el.renderable.TemplateEngine;
import org.jmouse.el.renderable.TemplateRenderer;
import org.jmouse.el.renderable.loader.StringLoader;

import java.util.List;

public class Smoke {

    public static void main(String[] arguments) {
        ExpressionLanguage el = new ExpressionLanguage();
        EvaluationContext context = el.newContext();

        context.setValue("f_textarea-00005-id", 22f);
        context.setValue("V", 130f);
        context.setValue("codes", List.of("A", "B", "C"));
        context.setValue("createdAt", "2026-07-07 12:00:01");

        el.evaluate("createdAt | instant <= now() | plusDays(3)", context);
        el.evaluate("'A' in codes", context);
        el.evaluate("'Low' in ['High', 'Low', 'Unknown']");



//        el.evaluate("$I2:f_textarea-00005-id", context);
//        el.evaluate("I2 / V", context);
//        el.evaluate("1D");
//        el.evaluate("[1, 2, 3, 4, 5] is contains(6)");

        TemplateEngine engine = new TemplateEngine();
        TemplateRenderer renderer = new TemplateRenderer(engine);
        engine.setLoader(new StringLoader());

//        Template template = engine.getTemplate("{% set N = 33 %}{% for k in 1..N %}{% if N % k == 0 %}{{ k }} {% endif %}{% endfor %}");
//        Template template = engine.getTemplate("{% do set('cnt', 1) %}");
        Template template = engine.getTemplate("{{ setVariable('a', 1) }}");
//        Template template = engine.getTemplate("{{ m:exp(33D) }}");
//        Template template = engine.getTemplate("{% if ('a' in ['a', 'b', 'c']) %}ok{% endif %}");

        EvaluationContext evaluationContext = template.newContext();

        MethodImporter.importMethod(Math.class, "m", evaluationContext);

        renderer.render(template, evaluationContext);
    }

}
