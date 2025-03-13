package org.jmouse.template.parser;

import org.jmouse.template.OperatorContainer;
import org.jmouse.template.extension.*;
import org.jmouse.template.lexer.Token;

public class DefaultParserContext implements ParserContext {

    private final ExtensionContainer extensions;
    private       ParserOptions      options;

    public DefaultParserContext() {
        this.extensions = new StandardExtensionContainer();
    }

    @Override
    public TagParser getTagParser(String name) {
        return extensions.getTagParser(name);
    }

    @Override
    public void addTagParser(TagParser parser) {
        extensions.addTagParser(parser);
    }

    @Override
    public Parser getParser(Class<? extends Parser> type) {
        return extensions.getParser(type);
    }

    @Override
    public void addParser(Parser parser) {
        extensions.addParser(parser);
    }

    @Override
    public Operator getOperator(Token.Type type) {
        return extensions.getOperator(type);
    }

    @Override
    public void addOperator(Operator operator) {
        extensions.addOperator(operator);
    }

    @Override
    public Function getFunction(String name) {
        return extensions.getFunction(name);
    }

    @Override
    public void addFunction(Function function) {
        extensions.addFunction(function);
    }

    @Override
    public Test getTest(String name) {
        return extensions.getTest(name);
    }

    @Override
    public void addTest(Test test) {
        extensions.addTest(test);
    }

    @Override
    public Filter getFilter(String name) {
        return extensions.getFilter(name);
    }

    @Override
    public void addFilter(Filter filter) {
        extensions.addFilter(filter);
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
