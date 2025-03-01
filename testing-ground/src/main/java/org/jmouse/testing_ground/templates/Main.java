package org.jmouse.testing_ground.templates;

import org.jmouse.template.TokenizableString;
import org.jmouse.template.lexer.*;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;
import org.jmouse.template.node.Node;
import org.jmouse.template.parser.ArithmeticParser;
import org.jmouse.template.parser.ParserContext;

import java.io.Reader;
import java.util.List;

public class Main {

    public static void main(String... arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Reader            reader    = loader.load("index");
//        TokenizableSource source    = new TokenizableString("index", reader);
//        TokenizableSource source    = new TokenizableString("index", "Hello {% for ab in   xyz  def %}! {{ users is not contains '123' }}");
        TokenizableSource source    = new TokenizableString("test-string", "Calculation: {{ 1 + 2 * 3 + 4 + 5 * 5 * 4 + 2 }} {# x++ --z v+=123 v-=111 #}");

        Lexer lexer = new TemplateLexer();

        TokenCursor cursor = lexer.tokenize(source);

        ParserContext parserContext = ParserContext.newContext();

        parserContext.addParser(new ArithmeticParser());

        Node root = parserContext.getParser(ArithmeticParser.class).parse(cursor, parserContext);

        System.out.println(root);

    }

}
