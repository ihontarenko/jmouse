package org.jmouse.template.parser;

import org.jmouse.template.ExtensionContainer;

public class DefaultParserContext implements ParserContext {

    private final ExtensionContainer<Parser>           parsers;
    private final ExtensionContainer<ExpressionParser> expressionParsers;
    private       ParserOptions                        options;

    public DefaultParserContext() {
        this.expressionParsers = new ExpressionParserContainer();
        this.parsers = new GlobalParserContainer();
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
    public Parser getParser(String name) {
        return parsers.get(name);
    }

    @Override
    public void addParser(Parser parser) {
        parsers.register(parser);
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
