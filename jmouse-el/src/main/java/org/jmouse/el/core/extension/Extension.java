package org.jmouse.el.core.extension;

import org.jmouse.el.core.parser.Parser;
import org.jmouse.el.core.parser.TagParser;

import java.util.Collections;
import java.util.List;

/**
 * 🔌 Represents an extension that provides additional functionality to the template engine.
 * Extensions can include custom tag parsers, expression parsers, operators, functions, tests, and filters.
 * <p>
 * By default, all methods return empty lists, meaning the extension does not provide any additional features
 * unless explicitly overridden.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public interface Extension {

    /**
     * 📌 Returns a list of custom tag parsers provided by this extension.
     *
     * @return a list of {@link TagParser} instances, or an empty list if none are provided.
     */
    default List<TagParser> getTagParsers() {
        return Collections.emptyList();
    }

    /**
     * 📌 Returns a list of expression parsers provided by this extension.
     *
     * @return a list of {@link Parser} instances, or an empty list if none are provided.
     */
    default List<Parser> getParsers() {
        return Collections.emptyList();
    }

    /**
     * ➕ Returns a list of custom operators added by this extension.
     *
     * @return a list of {@link Operator} instances, or an empty list if none are provided.
     */
    default List<Operator> getOperators() {
        return Collections.emptyList();
    }

    /**
     * 🔤 Returns a list of custom functions added by this extension.
     *
     * @return a list of {@link Function} instances, or an empty list if none are provided.
     */
    default List<Function> getFunctions() {
        return Collections.emptyList();
    }

    /**
     * ✅ Returns a list of custom tests (boolean expressions) added by this extension.
     *
     * @return a list of {@link Test} instances, or an empty list if none are provided.
     */
    default List<Test> getTests() {
        return Collections.emptyList();
    }

    /**
     * 🎨 Returns a list of custom filters added by this extension.
     *
     * @return a list of {@link Filter} instances, or an empty list if none are provided.
     */
    default List<Filter> getFilters() {
        return Collections.emptyList();
    }
}
