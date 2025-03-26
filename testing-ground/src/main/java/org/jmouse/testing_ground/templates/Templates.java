package org.jmouse.testing_ground.templates;

import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.*;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.RenderableEntity;
import org.jmouse.template.StandardTemplate;
import org.jmouse.template.TemplateCoreExtension;
import org.jmouse.template.lexer.TemplateRecognizer;
import org.jmouse.template.lexer.TemplateTokenizer;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;
import org.jmouse.template.node.BlockNode;
import org.jmouse.template.node.ContainerNode;
import org.jmouse.template.node.ExtendsNode;
import org.jmouse.template.node.RawTextNode;
import org.jmouse.template.parsing.TemplateParser;

import java.io.Reader;
import java.io.StringWriter;

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
        RenderableEntity entity  = new StandardTemplate(string);

        compiled.execute(node -> {
            if (node instanceof BlockNode block) {
                System.out.println(block);
            }
        });

        if (compiled instanceof ContainerNode body) {
            StringWriter writer = new StringWriter();
            Content content = Content.writer(writer);
            body.setOnlySafeNodes(true);
            body.setSafeNodes(RawTextNode.class, ExtendsNode.class);
            body.render(content, entity, evaluationContext);
        }

        System.out.println(compiled);
    }

}
