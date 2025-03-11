package org.jmouse.template.extension;

import org.jmouse.template.lexer.Token;
import org.jmouse.template.parser.ExpressionParser;
import org.jmouse.template.parser.Parser;

public interface Extension {

    ExpressionParser getExpressionParser(String name);

    void addExpressionParser(ExpressionParser parser);

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

}
