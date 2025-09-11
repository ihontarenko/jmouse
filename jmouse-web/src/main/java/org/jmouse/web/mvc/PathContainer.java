package org.jmouse.web.mvc;

import org.jmouse.core.Streamable;

import java.util.List;
import java.util.Map;

/**
 * 🧭 Abstraction for a parsed request path.
 *
 * <p>A {@code PathContainer} splits a raw path string into a sequence
 * of {@link Element}s: {@link Separator}s and {@link PathElement}s.
 * Supports matrix parameters within path segments.</p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * PathContainer pc = SimplePathContainer.parse("/user;id=42/profile");
 *
 * for (PathContainer.Element e : pc.elements()) {
 *     System.out.println(e.value());
 * }
 * }</pre>
 */
public interface PathContainer {

    /**
     * 📚 Return all path elements (separators + segments).
     *
     * @return immutable list of all {@link Element}s
     */
    List<Element> elements();

    /**
     * 🎯 Return all path elements of the given type.
     *
     * <p>Convenience method to filter {@link #elements()} by class.</p>
     *
     * @param elementType target element subtype (e.g. {@link Separator}, {@link PathElement})
     * @return list of elements of the specified type (may be empty)
     */
    default List<Element> elements(Class<? extends Element> elementType) {
        return Streamable.of(elements()).filter(elementType::isInstance).toList();
    }

    /**
     * ✂️ Return a sublist of elements.
     *
     * @param start start index (inclusive)
     * @param end   end index (exclusive)
     * @return sublist of elements
     */
    default List<Element> subElements(int start, int end) {
        return elements().subList(start, end);
    }

    /**
     * ➖ Separator element, e.g. "/" or ".".
     */
    interface Separator extends Element {

        /**
         * @return the raw separator string
         */
        String separator();
    }

    /**
     * 📦 Path segment element, optionally with matrix parameters.
     *
     * <p>Example: {@code "user;id=42"} → path: {@code "user"},
     * parameters: {@code {id:[42]}}.</p>
     */
    interface PathElement extends Element {

        /**
         * @return the literal path segment (without parameters)
         */
        String path();

        /**
         * @return matrix parameters, empty if none
         */
        Map<String, List<String>> parameters();
    }

    /**
     * 🔹 Base interface for all path elements.
     */
    interface Element {

        /**
         * @return the raw string value of this element
         */
        String value();
    }

    /**
     * ⚙️ Parsing options for {@link PathContainer}.
     *
     * @param separator       path separator character (e.g. '/')
     * @param parseParameters whether to parse matrix params
     */
    record Options(char separator, boolean parseParameters) {

        /**
         * Default separator: {@code '/'}
         */
        public static final char DEFAULT_SEPARATOR = '/';

        /**
         * 🔧 Create custom options.
         */
        public static Options create(char separator, boolean parseMatrix) {
            return new Options(separator, parseMatrix);
        }

        /**
         * 📦 Default options: separator = '/', parse matrix = true.
         */
        public static Options defaults() {
            return new Options(DEFAULT_SEPARATOR, true);
        }
    }
}
