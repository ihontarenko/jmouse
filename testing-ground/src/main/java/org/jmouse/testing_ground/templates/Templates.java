package org.jmouse.testing_ground.templates;

import org.jmouse.core.bind.ObjectAccessor;
import org.jmouse.core.bind.PropertyPath;
import org.jmouse.core.bind.accessor.JavaBeanAccessor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanIntrospector;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ExpressionLanguageValuesResolver;
import org.jmouse.el.evaluation.ReflectionClassPropertyResolver;
import org.jmouse.el.renderable.*;
import org.jmouse.el.renderable.evaluation.LoopVariables;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.testing_ground.binder.dto.Book;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Templates {

    public static void main(String[] arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".j.html");

        Engine engine = new TemplateEngine();

        engine.setLoader(loader);

//        Template          template = engine.getTemplate("site");
        Template          template = engine.getTemplate("benchmark");
        Renderer          renderer = new TemplateRenderer(engine);
        EvaluationContext context  = template.newContext();

        context.setValue("book", getBook("Stephen King", "The Shining"));
        context.setValue("books", getBookList());
        context.setValue("array", new String[]{"a", "b", "c"});
        context.setValue("map", new HashMap<>() {{
            put("key1", "valueA");
            put("key2", "valueB");
        }});
        context.setValue("list", new ArrayList<>() {{
            add(123);
            add(456);
            add(789);
        }});
        context.setValue("string", "Hello World");
        context.setValue("items", Stock.dummyItems());
        context.setValue("stock", Stock.dummyItems().get(0));

        context.getVirtualProperties().addVirtualProperty(new ReflectionClassPropertyResolver());

        Content content = renderer.render(template, context);

        System.out.println(content.toString().length());

        long start = System.currentTimeMillis();
        long spend = 0;
        int  times = 0;

        ObjectAccessor accessor = context.getValueResolver().getAccessor();

        while (spend < 1000) {
            times++;
            renderer.render(template, context);
            spend = System.currentTimeMillis() - start;

//            for (int i = 0; i < 20; i++) {
//                context.getValue("stock.name");
//                context.getValue("stock.name2");
//                context.getValue("stock.symbol");
//                context.getValue("stock.url");
//                context.getValue("stock.ratio");
//                context.getValue("stock.name");
//                context.getValue("stock.name2");
//                context.getValue("stock.symbol");
//                context.getValue("stock.url");
//                context.getValue("stock.ratio");
//                context.getValue("stock.name");
//                context.getValue("stock.name2");
//                context.getValue("stock.symbol");
//                context.getValue("stock.url");
//                context.getValue("stock.ratio");
//                context.getValue("stock.ratio");
//                context.getValue("stock.name");
//                context.getValue("stock.name2");
//                context.getValue("stock.symbol");
//                context.getValue("stock.url");
//                context.getValue("stock.ratio");
//
//            }

        }

        System.out.println("times: " + times);
        System.out.println(JavaBeanAccessor.CACHED_DESCRIPTORS);
        System.out.println(PropertyPath.CACHE);
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
