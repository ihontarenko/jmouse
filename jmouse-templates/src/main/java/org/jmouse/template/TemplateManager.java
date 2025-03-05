package org.jmouse.template;

import org.jmouse.template.loader.TemplateLoader;
import org.jmouse.template.parser.ExpressionParser;

public interface TemplateManager {

    Template getTemplate(String name);

    Template getTemplate(String name, TemplateLoader<String> loader);

    ExpressionParser registerExpressionParser(ExpressionParser parser);

}
