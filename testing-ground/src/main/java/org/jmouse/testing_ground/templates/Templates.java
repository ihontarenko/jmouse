package org.jmouse.testing_ground.templates;

import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ReflectionClassPropertyResolver;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.testing_ground.binder.dto.Book;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Templates {

    public static void main(String[] args) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".j.html");

        Engine engine = new TemplateEngine();

        engine.setLoader(loader);

        Template          template = engine.getTemplate("site");
        Renderer          renderer = new TemplateRenderer(engine);
        EvaluationContext context  = template.newContext();

        context.setValue("book", getBook("Stephen King", "The Shining"));
        context.setValue("books", getBookList());
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

    private static Book getBook(String author, String title) {
        Book book = new Book();

        book.setTitle(title);
        book.setAuthor(author);

        return book;
    }

    private static List<Book> getBookList() {
        List<Book> books = new ArrayList<>();

        books.add(getBook("J.K. Rowling", "Harry Potter and the Sorcerer's Stone"));
        books.add(getBook("Suzanne Collins", "The Hunger Games"));
        books.add(getBook("Stephen King", "The Shining"));
        books.add(getBook("Gillan Flynn", "Gone Girl"));
        books.add(getBook("Dan Brown", "The Da Vinci Code"));
        books.add(getBook("John Green", "The Fault in Our Stars"));
        books.add(getBook("Veronica Roth", "Divergent"));
        books.add(getBook("Ernest Cline", "Ready Player One"));
        books.add(getBook("E.L. James", "Fifty Shades of Grey"));
        books.add(getBook("Stieg Larsson", "The Girl with the Dragon Tattoo"));

        return books;
    }

}
