package org.jmouse.testing_ground.templates;

import org.jmouse.el.core.evaluation.EvaluationContext;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.testing_ground.binder.dto.Book;

import java.util.Map;

public class Templates {

    public static void main(String[] args) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Engine engine = new LeafsEngine();

        engine.setLoader(loader);

        Template template = engine.getTemplate("sub_sub");
        Renderer renderer = new Renderer.Default(engine);
        EvaluationContext context = template.newContext();

        context.setValue("renderer", new Book());

        Content content = renderer.render(template, context);

        System.out.println(content.toString());
    }

}
