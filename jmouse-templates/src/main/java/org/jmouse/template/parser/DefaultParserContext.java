package org.jmouse.template.parser;

import org.jmouse.template.ExtensionContainer;
import org.jmouse.template.OperatorContainer;
import org.jmouse.template.extension.Operator;
import org.jmouse.template.lexer.Token;

public class DefaultParserContext implements ParserContext {

    private final ExtensionContainer<Token.Type, Operator>            operators;
    private final ExtensionContainer<Class<? extends Parser>, Parser> parsers;
    private final ExtensionContainer<String, TagParser>               tags;
    private       ParserOptions                                       options;

    public DefaultParserContext() {
        this.parsers = new ParserContainer();
        this.tags = new TagParserContainer();
        this.operators = new OperatorContainer();
    }

    @Override
    public TagParser getTagParser(String name) {
        return tags.get(name);
    }

    @Override
    public void addTagParser(TagParser parser) {
        tags.register(parser);
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
