package org.jmouse.testing_ground.templates;

import org.jmouse.template.TokenizableString;
import org.jmouse.template.lexer.*;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;
import org.jmouse.template.node.BasicNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.parser.*;
import org.jmouse.template.parser.global.OperatorParser;
import org.jmouse.template.parser.global.RootParser;

import java.io.Reader;

public class Main {

    public static void main(String... arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Boolean a = null;

        System.out.println(a);

        Reader            reader    = loader.load("simple");
//        TokenizableSource source    = new TokenizableString("default.html", reader);
//        TokenizableSource source    = new TokenizableString("simple.html", reader);
//        TokenizableSource source    = new TokenizableString("index", "Hello {% for ab in   xyz  def %}! {{ users is not contains '123' }}");
//        TokenizableSource source    = new TokenizableString("test-string", "Calculation: {{ 1 + 2 * 3 + 4 + 5 * 5 * 4 + 2 }}");
//        TokenizableSource source    = new TokenizableString("test-string", "Calculation: {{ --1 + 2++ * 3^2 (2 + 6) }}");
//        TokenizableSource source    = new TokenizableString("test-string", "Calculation: {{ 1 + 2++}} {# x++ --z v+=123 v-=111 #}");
        TokenizableSource source    = new TokenizableString("test-string", "Calculation: {{ min() + 1 / user.id (low(), high(1, 2^3)) }}");

        Lexer lexer = new TemplateLexer();

        TokenCursor cursor = lexer.tokenize(source);

        ParserContext parserContext = new DefaultParserContext();



        parserContext.addParser(new OperatorParser());
        parserContext.addParser(new RootParser());

        cursor.next();
        Node root = BasicNode.forToken(new Token("Container", BasicToken.T_UNKNOWN, 0, 0, 0));
        parserContext.getParser(RootParser.class).parse(cursor, root, parserContext);

        System.out.println(root);

    }

}
