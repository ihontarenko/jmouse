package org.jmouse.el.extension;

import org.jmouse.core.bind.AttributeResolver;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.parser.TagParser;
import org.jmouse.el.parser.Parser;
import org.jmouse.core.Sorter;

import java.util.List;

/**
 * 🛠️ Manages various extensions used in the view engine, including functions, tests, filters, operators, and parsers.
 * <p>
 * This container allows dynamic registration and retrieval of extensions by their name or type,
 * making it a central registry for managing custom behaviors.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface ExtensionContainer {

    List<AttributeResolver> getAttributeResolvers();

    /**
     * 🔖 Retrieves a registered tag parser by its name.
     *
     * @param name the name of the tag parser
     * @return the {@link TagParser} instance, or {@code null} if not found
     */
    TagParser getTagParser(String name);

    /**
     * ➕ Adds a new tag parser to the container.
     *
     * @param parser the {@link TagParser} instance to register
     */
    void addTagParser(TagParser parser);

    /**
     * 🔍 Retrieves a registered expression parser by its class type.
     *
     * @param type the class type of the parser
     * @return the {@link Parser} instance, or {@code null} if not found
     */
    Parser getParser(Class<? extends Parser> type);

    /**
     * ➕ Adds a new expression parser to the container.
     *
     * @param parser the {@link Parser} instance to register
     */
    void addParser(Parser parser);

    /**
     * 🔢 Retrieves a registered operator by its token type.
     *
     * @param type the token type representing the operator
     * @return the {@link Operator} instance, or {@code null} if not found
     */
    Operator getOperator(Token.Type type);

    /**
     * ➕ Adds a new operator to the container.
     *
     * @param operator the {@link Operator} instance to register
     */
    void addOperator(Operator operator);

    /**
     * 🔤 Retrieves a registered function by its name.
     *
     * @param name the function name
     * @return the {@link Function} instance, or {@code null} if not found
     */
    Function getFunction(String name);

    /**
     * ➕ Adds a new function to the container.
     *
     * @param function the {@link Function} instance to register
     */
    void addFunction(Function function);

    /**
     * ✅ Retrieves a registered test by its name.
     *
     * @param name the test name
     * @return the {@link Test} instance, or {@code null} if not found
     */
    Test getTest(String name);

    /**
     * ➕ Adds a new test to the container.
     *
     * @param test the {@link Test} instance to register
     */
    void addTest(Test test);

    /**
     * 🎨 Retrieves a registered filter by its name.
     *
     * @param name the filter name
     * @return the {@link Filter} instance, or {@code null} if not found
     */
    Filter getFilter(String name);

    /**
     * ➕ Adds a new filter to the container.
     *
     * @param filter the {@link Filter} instance to register
     */
    void addFilter(Filter filter);

    /**
     * 🔄 Imports all available extensions from the given {@link Extension} instance.
     * <p>
     * This method automatically registers all functions, tests, filters, operators,
     * tag parsers, and expression parsers provided by the extension.
     * </p>
     *
     * @param extension the extension to import
     */
    default void importExtension(Extension extension) {
        extension.getTagParsers().forEach(this::addTagParser);
        extension.getFunctions().forEach(this::addFunction);
        extension.getTests().forEach(this::addTest);
        extension.getFilters().forEach(this::addFilter);
        extension.getOperators().forEach(this::addOperator);
        extension.getParsers().forEach(this::addParser);
        getAttributeResolvers().addAll(extension.getAttributeResolvers());
        Sorter.sort(getAttributeResolvers());
    }
}
