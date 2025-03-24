package org.jmouse.testing_ground.templates;

import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.*;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.template.TemplateCoreExtension;
import org.jmouse.template.lexer.TemplateRecognizer;
import org.jmouse.template.lexer.TemplateTokenizer;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;
import org.jmouse.template.parsing.TemplateParser;

import java.io.Reader;

public class Templates {

    public static void main(String[] args) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        Reader            reader = loader.load("sub");
        TokenizableSource string = new StringSource("sub", reader);

        //////////////////////////

        TemplateTokenizer tokenizer = new TemplateTokenizer(new TemplateRecognizer());
        Lexer             lexer     = new DefaultLexer(tokenizer);

        TokenCursor cursor = lexer.tokenize(string);

        cursor.next();

        ParserContext parserContext = new DefaultParserContext();

        parserContext.importExtension(new TemplateCoreExtension());

        EvaluationContext evaluationContext = new DefaultEvaluationContext();

        evaluationContext.getExtensions().importExtension(new TemplateCoreExtension());

        Node compiled = parserContext.getParser(TemplateParser.class).parse(cursor, parserContext);

        System.out.println(compiled);
    }

}
