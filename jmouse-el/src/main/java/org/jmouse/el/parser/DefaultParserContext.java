package org.jmouse.el.parser;

import org.jmouse.core.context.ContextScope;
import org.jmouse.el.extension.*;
import org.jmouse.core.access.AttributeResolver;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.type.ValueType;

import java.util.List;

public class DefaultParserContext implements ParserContext {

    private final ExtensionContainer                    extensions;
    private final ContextScope<Class<? extends Parser>> scope = new ContextScope<>();

    /**
     * ⚠️ Per thread, like {@link ContextScope}. A parser sets options around its own argument parse and
     * clears them afterwards; one context is shared by every thread parsing through the same language, so a
     * single field let one parse clear — or replace — another's options in the middle of reading them.
     */
    private final ThreadLocal<ParserOptions>            options = new ThreadLocal<>();

    public DefaultParserContext() {
        this(new StandardExtensionContainer());
    }

    public DefaultParserContext(ExtensionContainer extensions) {
        this.extensions = extensions;
    }

    @Override
    public ContextScope<Class<? extends Parser>> getContextScope() {
        return scope;
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
    public Parser getParser(TokenCursor cursor) {
        return extensions.getParser(cursor);
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
    public ValueType getType(String name) {
        return extensions.getType(name);
    }

    @Override
    public List<ValueType> getTypes() {
        return extensions.getTypes();
    }

    @Override
    public void addType(ValueType type) {
        extensions.addType(type);
    }

    @Override
    public ExtensionContainer getExtensionContainer() {
        return extensions;
    }

    @Override
    public ParserOptions getOptions() {
        return options.get();
    }

    @Override
    public void setOptions(ParserOptions options) {
        this.options.set(options);
    }

    @Override
    public void clearOptions() {
        this.options.remove();
    }
}
