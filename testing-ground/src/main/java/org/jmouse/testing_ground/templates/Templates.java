package org.jmouse.testing_ground.templates;

import org.jmouse.el.StringSource;
import org.jmouse.el.evaluation.DefaultEvaluationContext;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.lexer.*;
import org.jmouse.el.parser.DefaultParserContext;
import org.jmouse.el.parser.ParserContext;
import org.jmouse.el.rendering.Content;
import org.jmouse.el.rendering.Template;
import org.jmouse.template.*;
import org.jmouse.template.lexer.TemplateRecognizer;
import org.jmouse.template.lexer.TemplateTokenizer;
import org.jmouse.template.loader.ClasspathLoader;
import org.jmouse.template.loader.TemplateLoader;

import java.io.Reader;

public class Templates {

    public static void main(String[] args) {
        TemplateLoader<String> loader = new ClasspathLoader();

        loader.setPrefix("templates/");
        loader.setSuffix(".html");

        EvaluationContext evaluationContext = new DefaultEvaluationContext();

        evaluationContext.getExtensions().importExtension(new TemplateCoreExtension());

        Engine engine = new LeafsEngine();

        engine.setLoader(loader);

        Template template = engine.getTemplate("sub");

        Content content = template.evaluate(evaluationContext);

        System.out.println(content.toString());
    }

}
