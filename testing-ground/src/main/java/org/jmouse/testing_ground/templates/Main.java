package org.jmouse.testing_ground.templates;

import org.jmouse.core.bind.Bind;
import org.jmouse.core.bind.PropertyPath;
import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.evaluation.ScopedChain;
import org.jmouse.el.lexer.recognizer.CompositeRecognizer;
import org.jmouse.el.lexer.recognizer.EnumTokenRecognizer;
import org.jmouse.el.lexer.recognizer.Recognizer;
import org.jmouse.el.node.ExpressionNode;
import org.jmouse.el.parser.OperatorParser;
import org.jmouse.template.CoreExtension;
import org.jmouse.el.lexer.*;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.template.TemplateToken;
import org.jmouse.template.lexer.TemplateRecognizer;
import org.jmouse.template.lexer.TemplateTokenizer;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;
import org.jmouse.el.node.Node;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String... arguments) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Reader            reader    = loader.load("simple");
//        TokenizableSource string    = new StringSource("default.html", reader);
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
        TokenizableSource string    = new StringSource("string-test", "<h1>{% if x is even and y is not inset(1, 2, 3) || a > 1 and a < 10 and x is even or data.users[0].name is odd %}</h1>");
//        TokenizableSource string    = new StringSource("string-test", "<h1>{{ data.users[0].name }}</h1>");

        CompositeRecognizer recognizer = new CompositeRecognizer();
        recognizer.addRecognizer(new EnumTokenRecognizer<>(BasicToken.class, 20));
        recognizer.addRecognizer(new EnumTokenRecognizer<>(TemplateToken.class, 10));
        recognizer.addRecognizer(new TemplateRecognizer());

        TemplateTokenizer tokenizer = new TemplateTokenizer(recognizer);
        Lexer lexer = new DefaultLexer(tokenizer);

        TokenCursor cursor = lexer.tokenize(string);

//        TokenizableSource elString = new StringSource("el-string", "1 | toInt(-1) | toBigInt is not int(1++) || x is even or data.users[0].name | trim is odd");
//        TokenizableSource elString = new StringSource("el-string", "123 + user[0].level / 22");
//        TokenizableSource elString = new StringSource("el-string", "22 / 7");
//        TokenizableSource elString = new StringSource("el-string", "22 / 7 > 4");
        TokenizableSource elString = new StringSource("el-string", "user[0].name");
        Recognizer<Token.Type, RawToken> elr = new EnumTokenRecognizer<>(BasicToken.class, 20);
        Lexer elLexer = new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), elr));

        Map<String, Object> source = Map.of("name", "John");
        Map<String, Object> destination = new HashMap<>();

        destination.put("level", 10);
        destination.put("name", "Lalka");

        Bind.with(source).to(destination);

        PropertyPath path = PropertyPath.forPath("users[0].name");

        TokenCursor elCursor = elLexer.tokenize(elString);

        ParserContext parserContext = new DefaultParserContext();

        parserContext.importExtension(new CoreExtension());

        cursor.next();
        elCursor.next();
        Node compiled = parserContext.getParser(OperatorParser.class).parse(elCursor, parserContext);

        EvaluationContext evaluationContext = new DefaultEvaluationContext();

        evaluationContext.getExtensions().importExtension(new CoreExtension());

        ScopedChain scopedChain = evaluationContext.getScopedChain();

        scopedChain.setValue("user", List.of(Map.of("name", "Root!", "level", 333)));
        scopedChain.push();
        scopedChain.setValue("user", List.of(destination));

        if (compiled instanceof ExpressionNode expressionNode) {
            Object value = expressionNode.evaluate(evaluationContext);
            System.out.println(STR."value: \{value}");
        }

        System.out.println(compiled);
    }

}
