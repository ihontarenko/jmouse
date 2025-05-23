package org.jmouse.el.parser;

import org.jmouse.el.extension.*;
import org.jmouse.core.bind.AttributeResolver;
import org.jmouse.el.lexer.Token;

import java.util.List;

public class DefaultParserContext implements ParserContext {

    private final ExtensionContainer extensions;
    private       ParserOptions      options;

    public DefaultParserContext() {
        this(new StandardExtensionContainer());
    }

    public DefaultParserContext(ExtensionContainer extensions) {
        this.extensions = extensions;
    }

    @Override
    public List<AttributeResolver> getAttributeResolvers() {
        return extensions.getAttributeResolvers();
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
    public ExtensionContainer getExtensionContainer() {
        return extensions;
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
