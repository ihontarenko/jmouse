package org.jmouse.template.extension;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.parser.TagParser;
import org.jmouse.template.parser.Parser;

import java.util.List;

public interface Extension {

    TagParser getTagParser(String name);

    void addTagParser(TagParser parser);

    Parser getParser(Class<? extends Parser> type);

    void addParser(Parser parser);

    Operator getOperator(Token.Type type);

    void addOperator(Operator operator);

    Function getFunction(String name);

    void addFunction(Function function);

    Test getTest(String name);

    void addTest(Test test);

    Filter getFilter(String name);

    void addFilter(Filter filter);

    List<TagParser> getExpressionParsers();

    List<Parser> getParsers();

    List<Operator> getOperators();

    List<Function> getFunctions();

    List<Test> getTests();

    List<Filter> getFilters();

}
