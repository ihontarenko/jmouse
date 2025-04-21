package org.jmouse.el.extension;

import org.jmouse.el.extension.attribute.AttributeResolver;
import org.jmouse.el.parser.Parser;
import org.jmouse.el.parser.TagParser;

import java.util.Collections;
import java.util.List;

/**
 * 🔌 Core extension point for the template engine.
 * <p>
 * Allows plug‑in of custom behavior such as:
 * <ul>
 *   <li>Attribute resolvers ({@link AttributeResolver})</li>
 *   <li>Tag parsers ({@link TagParser})</li>
 *   <li>Expression parsers ({@link Parser})</li>
 *   <li>Operators</li>
 *   <li>Functions</li>
 *   <li>Tests (boolean predicates)</li>
 *   <li>Filters (post‑processing functions)</li>
 * </ul>
 * </p>
 * <p>
 * By default, no additional features are provided. Extensions should override
 * the relevant methods to contribute their functionality.
 * </p>
 */
public interface Extension {

    /**
     * Returns custom attribute resolvers to handle XML/HTML‑style attributes.
     *
     * @return a list of {@link AttributeResolver} instances, or an empty list
     */
    default List<AttributeResolver> getAttributeResolvers() {
        return Collections.emptyList();
    }

    /**
     * Returns custom tag parsers for template directives.
     *
     * @return a list of {@link TagParser} instances, or an empty list
     */
    default List<TagParser> getTagParsers() {
        return Collections.emptyList();
    }

    /**
     * Returns custom expression parsers for embedded code.
     *
     * @return a list of {@link Parser} instances, or an empty list
     */
    default List<Parser> getParsers() {
        return Collections.emptyList();
    }

    /**
     * Returns custom operators (e.g., "+", "-", "==", "~", etc.).
     *
     * @return a list of {@link Operator} instances, or an empty list
     */
    default List<Operator> getOperators() {
        return Collections.emptyList();
    }

    /**
     * Returns custom functions usable in expressions.
     *
     * @return a list of {@link Function} instances, or an empty list
     */
    default List<Function> getFunctions() {
        return Collections.emptyList();
    }

    /**
     * Returns custom tests (boolean checks) usable in expressions.
     *
     * @return a list of {@link Test} instances, or an empty list
     */
    default List<Test> getTests() {
        return Collections.emptyList();
    }

    /**
     * Returns custom filters for post‑processing of values.
     *
     * @return a list of {@link Filter} instances, or an empty list
     */
    default List<Filter> getFilters() {
        return Collections.emptyList();
    }
}
