package org.jmouse.testing_ground.templates;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.Node;
import org.jmouse.el.node.Visitor;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.testing_ground.binder.dto.Book;

public class Templates {

    public static void main(String[] args) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Engine engine = new TemplateEngine();

        engine.setLoader(loader);

        Template          template = engine.getTemplate("user");
        Renderer          renderer = new TemplateRenderer(engine);
        EvaluationContext context  = template.newContext();

        template.getRoot();

        context.setValue("book", getBook());
        context.setValue("name", getBook().getAuthor());

        template.getRoot().accept(new Visitor() {
            @Override
            public void visit(Node node) {
                System.out.println(node);
            }
        });

        Content content = renderer.render(template, context);

        System.out.println(content.toString());
    }

    private static Book getBook() {
        Book book = new Book();

        book.setTitle("The Lord of the Rings");
        book.setAuthor("J. R. R. Tolkien");

        return book;
    }

}
