package org.jmouse.testing_ground.templates;

import org.jmouse.core.bind.VirtualProperty;
import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopedChain;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;
import org.jmouse.el.lexer.recognizer.Recognizer;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.renderable.TemplateCoreExtension;
import org.jmouse.el.lexer.*;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.renderable.lexer.TemplateToken;
import org.jmouse.el.renderable.lexer.TemplateRecognizer;
import org.jmouse.el.renderable.lexer.TemplateTokenizer;
import org.jmouse.el.renderable.loader.ClasspathLoader;
import org.jmouse.el.renderable.loader.TemplateLoader;
import org.jmouse.el.node.Node;
import org.jmouse.el.renderable.parser.TemplateParser;
import org.jmouse.testing_ground.binder.dto.Book;

import java.io.Reader;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String... arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Reader            reader    = loader.load("default");
        TokenizableSource string    = new StringSource("default.html", reader);
//        TokenizableSource string    = new StringSource("simple.html", reader);
//        TokenizableSource string    = new StringSource("index", "Hello {% for ab in   xyz  def %}! {{ users is not contains '123' }}");
//        TokenizableSource string    = new StringSource("test-string", "Calculation: {{ 1 + 2 * 3 + 4 + 5 * 5 * 4 + 2 }}");
//        TokenizableSource string    = new StringSource("test-string", "Calculation: {{ --1 + 2++ * 3^2 (2 + 6) }}");
//        TokenizableSource string    = new StringSource("test-string", "Calculation: {{ 1 + 2++}} {# x++ --z v+=123 v-=111 #}");
//        TokenizableSource string    = new StringSource("test-string", "Calculation: {{ min() + 1 / user.id (low(), high(1, 2^3)) }}");
//        TokenizableSource string    = new StringSource("string-test", "<h1>{% min(1.1, false, null, min() | toBigInt | abs, 1++, 2 + 3 / min() * 2 ^ 2 * 2) + 2 %}</h1>");
//        TokenizableSource string    = new StringSource("string-test", "<h1>{{ 2 <= 3 || (1 > 0 && 2 < 3) >= 3 && 4 < min() or 5 % 2 != 0 }}</h1>");
//        TokenizableSource string    = new StringSource("string-test", "<h1>{{ 1 == 1 and 1 != 1 or 2 != 2 }}</h1>");
//        TokenizableSource string    = new StringSource("string-test", "<h1>{{ 2 + (2 + 2) * 2 / 3 (22 / 7) }}</h1>");
//        TokenizableSource string    = new StringSource("string-test", "<h1>{{ 1 (2 + 3) * 4 }}</h1>");

//        TokenizableSource string    = new StringSource("string-test", "<h1>{{ min(max() + 2 / 3 (1+1) ) }}</h1>");
//        TokenizableSource string    = new StringSource("string-test", "<h1>{% if x is even and y is not inset(1, 2, 3) || a > 1 and a < 10 and x is even or data.users[0].name is odd %}</h1>");
//        TokenizableSource string    = new StringSource("string-test", "<h1>{{ data.users[0].name }}</h1>");

        CompositeRecognizer recognizer = new CompositeRecognizer();
        recognizer.addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 20));
        recognizer.addRecognizer(new EnumTokenRecognizer<>(TemplateToken.class, 10));
        recognizer.addRecognizer(new TemplateRecognizer());

        TemplateTokenizer tokenizer = new TemplateTokenizer(recognizer);
        Lexer lexer = new DefaultLexer(tokenizer);

        TokenCursor cursor = lexer.tokenize(string);

//        TokenizableSource elString = new StringSource("lexer-string", "1 | toInt(-1) | toBigInt is not int(1++) || x is even or data.users[0].name | trim is odd");
//        TokenizableSource elString = new StringSource("lexer-string", "123 + user[0].level / 22");
//        TokenizableSource elString = new StringSource("lexer-string", "22 / 7");
//        TokenizableSource elString = new StringSource("lexer-string", "22 / 7 > 3");
//        TokenizableSource elString = new StringSource("lexer-string", "2 + (2 + 2) * 2 / 3 (22 / 7) is odd");
//        TokenizableSource elString = new StringSource("lexer-string", "++cnt / 2 is odd");
//        TokenizableSource elString = new StringSource("lexer-string", "[1, '2', 3.14] is array");
        TokenizableSource elString = new StringSource("lexer-string", "set('_map', {'name': 'John' | upper, 'level': 321 ** 7 / 33 | toBigDecimal, 'min': min(123, 111), 'aaa': 5643%123})");
//        TokenizableSource elString = new StringSource("lexer-string", "set('_name', book.full | upper)");
//        TokenizableSource elString = new StringSource("lexer-string", "set('_name', book.full | default('Unnamed') | upper)");
//        TokenizableSource elString = new StringSource("lexer-string", "user[0].book.title | default('qwe') | upper");
//        TokenizableSource elString = new StringSource("lexer-string", "'test123' is inset(1, 2, 'test123', 3)");
//        TokenizableSource elString = new StringSource("lexer-string", "{user[0].book.title | upper : user[0].name | upper} is map");
        Recognizer<Token.Type, RawToken> elr = new EnumTokenRecognizer<>(BasicToken.class, 20);
        Lexer elLexer = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), elr));

        TokenCursor elCursor = elLexer.tokenize(elString);

        ParserContext parserContext = new DefaultParserContext();

        parserContext.importExtension(new TemplateCoreExtension());

        cursor.next();
        elCursor.next();
        Node compiled = parserContext.getParser(TemplateParser.class).parse(cursor, parserContext);

        EvaluationContext evaluationContext = new DefaultEvaluationContext();

        evaluationContext.getExtensions().importExtension(new TemplateCoreExtension());

        ScopedChain scopedChain = evaluationContext.getScopedChain();

        evaluationContext.getVirtualProperties().addVirtualProperty(new VirtualProperty<Book>() {
            @Override
            public Class<Book> getType() {
                return Book.class;
            }

            @Override
            public String getName() {
                return "full";
            }

            @Override
            public void writeValue(Book object, Object value) {

            }

            @Override
            public Object readValue(Book book) {
                return STR."Author: \{book.getAuthor()}: Book: '\{book.getTitle()}' - \{book.getPages()} pages.";
            }

            @Override
            public boolean isReadable() {
                return true;
            }
        });

        Book book = new Book();
        book.setAuthor("John");
        book.setTitle("It");
        book.setPages(671);

        scopedChain.setValue("user", List.of(Map.of("name", "Root!", "active", true, "level", 333, "book", book)));
        scopedChain.setValue("cnt", 10D);

        if (compiled instanceof ExpressionNode expressionNode) {
            Object value = expressionNode.evaluate(evaluationContext);
            System.out.println(STR."value: \{value}");
        }

        System.out.println(compiled);
    }

}
