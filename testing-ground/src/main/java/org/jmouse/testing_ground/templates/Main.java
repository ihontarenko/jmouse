package org.jmouse.testing_ground.templates;

import org.jmouse.template.TokenizableString;
import org.jmouse.template.lexer.DefaultTokenizer;
import org.jmouse.template.lexer.Token;
import org.jmouse.template.lexer.TokenizableSource;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

public class Main {

    public static void main(String... arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Reader            reader    = loader.load("index");
//        TokenizableSource source    = new TokenizableString("index", reader);
        TokenizableSource source    = new TokenizableString("index", "Hello {% for ab in   xyz  def %}!");
        DefaultTokenizer  tokenizer = new DefaultTokenizer();

        List<Token> tokens = tokenizer.tokenize(source);

        for (TokenizableSource.Entry entry : source) {
            System.out.println(entry + ": " + entry.segment());
        }

        System.out.println(tokens.size());

    }

}
