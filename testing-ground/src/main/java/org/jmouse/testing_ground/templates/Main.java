package org.jmouse.testing_ground.templates;

import org.jmouse.template.TemplateSource;
import org.jmouse.template.lexer.*;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;

import java.io.Reader;
import java.util.List;

public class Main {

    public static void main(String... arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Reader         reader = loader.load("index");
        TemplateSource source = new TemplateSource("index", reader);

        //////////////////
        Tokenizer tokenizer = new DefaultTokenizer();

        List<Token> tokens = tokenizer.tokenize(source);

        System.out.println(tokens);
    }

}
