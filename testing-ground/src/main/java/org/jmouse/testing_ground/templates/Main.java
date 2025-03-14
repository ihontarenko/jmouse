package org.jmouse.testing_ground.templates;

import org.jmouse.template.CoreExtension;
import org.jmouse.template.TokenizableString;
import org.jmouse.template.lexer.*;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;
import org.jmouse.template.node.BasicNode;
import org.jmouse.template.node.Node;
import org.jmouse.template.parsing.*;
import org.jmouse.template.parsing.parser.RootParser;

import java.io.Reader;

public class Main {

    public static void main(String... arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Reader            reader    = loader.load("simple");
//        TokenizableSource string    = new TokenizableString("default.html", reader);
//        TokenizableSource string    = new TokenizableString("simple.html", reader);
//        TokenizableSource string    = new TokenizableString("index", "Hello {% for ab in   xyz  def %}! {{ users is not contains '123' }}");
//        TokenizableSource string    = new TokenizableString("test-string", "Calculation: {{ 1 + 2 * 3 + 4 + 5 * 5 * 4 + 2 }}");
//        TokenizableSource string    = new TokenizableString("test-string", "Calculation: {{ --1 + 2++ * 3^2 (2 + 6) }}");
//        TokenizableSource string    = new TokenizableString("test-string", "Calculation: {{ 1 + 2++}} {# x++ --z v+=123 v-=111 #}");
//        TokenizableSource string    = new TokenizableString("test-string", "Calculation: {{ min() + 1 / user.id (low(), high(1, 2^3)) }}");
//        TokenizableSource string    = new TokenizableString("string-test", "<h1>{% min(1.1, false, null, min() | toBigInt | abs, 1++, 2 + 3 / min() * 2 ^ 2 * 2) + 2 %}</h1>");
//        TokenizableSource string    = new TokenizableString("string-test", "<h1>{{ 2 <= 3 || (1 > 0 && 2 < 3) >= 3 && 4 < min() or 5 % 2 != 0 }}</h1>");
//        TokenizableSource string    = new TokenizableString("string-test", "<h1>{{ 1 == 1 and 1 != 1 or 2 != 2 }}</h1>");
//        TokenizableSource string    = new TokenizableString("string-test", "<h1>{{ 2 + (2 + 2) * 2 / 3 (22 / 7) }}</h1>");
//        TokenizableSource string    = new TokenizableString("string-test", "<h1>{{ 1 (2 + 3) * 4 }}</h1>");

        TokenizableSource string    = new TokenizableString("string-test", "<h1>{{ min(min(), 123) }}</h1>");
//        TokenizableSource string    = new TokenizableString("string-test", "<h1>{% if x is even || a > 1 or data.users[0].name is odd %}</h1>");
//        TokenizableSource string    = new TokenizableString("string-test", "<h1>{{ data.users[0].name }}</h1>");

        Lexer lexer = new TemplateLexer();

        TokenCursor cursor = lexer.tokenize(string);

        ParserContext parserContext = new DefaultParserContext();

        parserContext.importExtension(new CoreExtension());

        cursor.next();
        Node root = BasicNode.forToken(new Token("Container", BasicToken.T_UNKNOWN, 0, 0, 0));
        parserContext.getParser(RootParser.class).parse(cursor, root, parserContext);

        System.out.println(root);

    }

}
