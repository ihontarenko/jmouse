package org.jmouse.template.extension;

import org.jmouse.template.parser.ExpressionParser;
import org.jmouse.template.parser.Parser;

import java.util.List;

public interface ExtensionProvider {

    List<ExpressionParser> getExpressionParsers();

    List<Parser> getParsers();

    List<Operator> getOperators();

    List<Function> getFunctions();

    List<Test> getTests();

    List<Filter> getFilters();

}
