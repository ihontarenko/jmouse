package org.jmouse.testing_ground.templates;

import org.jmouse.template.TemplateSource;
import org.jmouse.template.TokenizedString;
import org.jmouse.template.lexer.*;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;

import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String... arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Reader         reader = loader.load("index");
        TemplateSource source = new TemplateSource("index", reader);

        source.shift(123);

        //////////////////
        Tokenizer<Token.Entry, Tokenizable> tokenizer = new DefaultTokenizer();

        List<Token.Entry> tokens = tokenizer.tokenize(source.text());

        Lexer lexer = new TemplateLexer(tokens);

        System.out.println(tokens.size());
    }

}
