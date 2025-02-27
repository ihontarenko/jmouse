package org.jmouse.testing_ground.templates;

import org.jmouse.template.StringSource;
import org.jmouse.template.lexer.DefaultTokenizer;
import org.jmouse.template.lexer.RawSplitter;
import org.jmouse.template.lexer.RawToken;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;

import java.io.Reader;
import java.util.List;

public class Main {

    public static void main(String... arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Reader reader = loader.load("index");

        StringSource source = new StringSource("index", reader);

        RawSplitter splitter = new RawSplitter();

        List<RawToken> rawTokens = splitter.split(source, 221, source.length());

        for (RawToken rawToken : rawTokens) {
            System.out.println(rawToken);
        }

//        System.out.println(source.getLineNumber(123));
//
//        DefaultTokenizer tokenizer = new DefaultTokenizer();
//
//        List<Token> tokens = tokenizer.tokenize(source);
//
//        System.out.println(tokens.size());

    }

}
