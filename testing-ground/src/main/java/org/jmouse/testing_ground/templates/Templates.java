package org.jmouse.testing_ground.templates;

import org.jmouse.core.bind.PropertyPath;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ReflectionClassPropertyResolver;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.testing_ground.binder.dto.Book;

import java.util.ArrayList;
import java.util.HashMap;

public class Templates {

    public static void main(String[] args) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".jel.html");

        Engine engine = new TemplateEngine();

        engine.setLoader(loader);

        Template          template = engine.getTemplate("simple");
        Renderer          renderer = new TemplateRenderer(engine);
        EvaluationContext context  = template.newContext();

        context.setValue("book", getBook());
        context.setValue("array", new String[]{"a", "b", "c"});
        context.setValue("map", new HashMap<>(){{
            put("key1", "valueA");
            put("key2", "valueB");
        }});
        context.setValue("list", new ArrayList<>(){{
            add(123);
            add(456);
            add(789);
        }});
        context.setValue("string", "Hello World");

        context.getVirtualProperties().addVirtualProperty(new ReflectionClassPropertyResolver());

        Content content = renderer.render(template, context);

        System.out.println(content.toString().length());
    }

    private static Book getBook() {
        Book book = new Book();

        book.setTitle("The Lord of the Rings");
        book.setAuthor("J. R. R. Tolkien");

        return book;
    }

}
