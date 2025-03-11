package org.jmouse.template.parser;

import org.jmouse.template.ExtensionContainer;
import org.jmouse.template.OperatorContainer;
import org.jmouse.template.extension.Operator;
import org.jmouse.template.lexer.Token;

public class DefaultParserContext implements ParserContext {

    private final ExtensionContainer<Token.Type, Operator>            operators;
    private final ExtensionContainer<Class<? extends Parser>, Parser> parsers;
    private final ExtensionContainer<String, ExpressionParser>        expressionParsers;
    private       ParserOptions                                       options;

    public DefaultParserContext() {
        this.expressionParsers = new ExpressionParserContainer();
        this.parsers = new ParserContainer();
        this.operators = new OperatorContainer();
    }

    @Override
    public ExpressionParser getExpressionParser(String name) {
        return expressionParsers.get(name);
    }

    @Override
    public void addExpressionParser(ExpressionParser parser) {
        expressionParsers.register(parser);
    }

    @Override
    public Parser getParser(Class<? extends Parser> type) {
        return parsers.get(type);
    }

    @Override
    public void addParser(Parser parser) {
        parsers.register(parser);
    }

    @Override
    public Operator getOperator(Token.Type type) {
        return operators.get(type);
    }

    @Override
    public void addOperator(Operator operator) {
        this.operators.register(operator);
    }

    @Override
    public ParserOptions getOptions() {
        return options;
    }

    @Override
    public void setOptions(ParserOptions options) {
        this.options = options;
    }

    @Override
    public void clearOptions() {
        this.options = null;
    }
}
