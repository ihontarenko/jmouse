package org.jmouse.el.extension;

import org.jmouse.el.ObjectContainer;
import org.jmouse.core.access.AttributeResolver;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.ParserContainer;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.TagParserContainer;

import java.util.ArrayList;
import java.util.List;

/**
 * 🏗️ Standard implementation of {@link ExtensionContainer}.
 * <p>
 * This container holds various view engine extensions, including functions, tests, filters,
 * operators, and parsers. It provides methods for managing and retrieving them dynamically.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class StandardExtensionContainer implements ExtensionContainer {

    private final ObjectContainer<Class<? extends Parser>, Parser> parsers;
    private final ObjectContainer<String, TagParser>               tags;
    private final ObjectContainer<Token.Type, Operator>            operators;
    private final ObjectContainer<String, Test>                    tests;
    private final ObjectContainer<String, Function>                functions;
    private final ObjectContainer<String, Filter>                  filters;
    private final List<AttributeResolver>                          attributeResolvers;

    /**
     * 🛠️ Constructs an empty {@code StandardExtensionContainer}.
     */
    public StandardExtensionContainer() {
        attributeResolvers = new ArrayList<>();
        tags = new TagParserContainer();
        parsers = new ParserContainer();
        tests = new TestContainer();
        functions = new FunctionContainer();
        filters = new FilterContainer();
        operators = new OperatorContainer();
    }

    @Override
    public List<AttributeResolver> getAttributeResolvers() {
        return attributeResolvers;
    }

    /**
     * 🔖 Retrieves a registered tag parser by its name.
     *
     * @param name the name of the tag parser
     * @return the {@link TagParser} instance, or {@code null} if not found
     */
    @Override
    public TagParser getTagParser(String name) {
        return tags.get(name);
    }

    /**
     * ➕ Adds a new tag parser to the container.
     *
     * @param parser the {@link TagParser} instance to register
     */
    @Override
    public void addTagParser(TagParser parser) {
        tags.register(parser);
    }

    /**
     * 🔍 Retrieves a registered parser by its class type.
     *
     * @param type the class type of the parser
     * @return the {@link Parser} instance, or {@code null} if not found
     */
    @Override
    public Parser getParser(Class<? extends Parser> type) {
        return parsers.get(type);
    }

    @Override
    public Parser getParser(TokenCursor cursor) {
        Parser parser = null;

        for (Parser candidate : parsers.values()) {
            if (candidate.supports(cursor)) {
                parser = candidate;
                break;
            }
        }

        return parser;
    }

    /**
     * ➕ Adds a new parser to the container.
     *
     * @param parser the {@link Parser} instance to register
     */
    @Override
    public void addParser(Parser parser) {
        parsers.register(parser);
    }

    /**
     * 🔢 Retrieves a registered operator by its token type.
     *
     * @param type the token type representing the operator
     * @return the {@link Operator} instance, or {@code null} if not found
     */
    @Override
    public Operator getOperator(Token.Type type) {
        return operators.get(type);
    }

    /**
     * ➕ Adds a new operator to the container.
     *
     * @param operator the {@link Operator} instance to register
     */
    @Override
    public void addOperator(Operator operator) {
        operators.register(operator);
    }

    /**
     * 🔤 Retrieves a registered function by its name.
     *
     * @param name the function name
     * @return the {@link Function} instance, or {@code null} if not found
     */
    @Override
    public Function getFunction(String name) {
        return functions.get(name);
    }

    /**
     * ➕ Adds a new function to the container.
     *
     * @param function the {@link Function} instance to register
     */
    @Override
    public void addFunction(Function function) {
        functions.register(function);
    }

    /**
     * ✅ Retrieves a registered test by its name.
     *
     * @param name the test name
     * @return the {@link Test} instance, or {@code null} if not found
     */
    @Override
    public Test getTest(String name) {
        return tests.get(name);
    }

    /**
     * ➕ Adds a new test to the container.
     *
     * @param test the {@link Test} instance to register
     */
    @Override
    public void addTest(Test test) {
        tests.register(test);
    }

    /**
     * 🎨 Retrieves a registered filter by its name.
     *
     * @param name the filter name
     * @return the {@link Filter} instance, or {@code null} if not found
     */
    @Override
    public Filter getFilter(String name) {
        return filters.get(name);
    }

    /**
     * ➕ Adds a new filter to the container.
     *
     * @param filter the {@link Filter} instance to register
     */
    @Override
    public void addFilter(Filter filter) {
        filters.register(filter);
    }

}
