package org.jmouse.web.mvc;

import org.jmouse.util.StringHelper;

import java.util.*;

import static java.util.Collections.emptyList;

/**
 * 🧭 Default {@link PathContainer} implementation that parses a path
 * into a sequence of {@link Element}s (separators and path segments with
 * optional matrix parameters).
 *
 * <p>Supports matrix params in segments, e.g. {@code /user;id=42/profile}.
 * The default separator is configurable via {@link Options} (e.g. {@code '/'} or {@code '.'}).</p>
 *
 * <h2>Examples</h2>
 * <pre>{@code
 * PathContainer p1 = SimplePathContainer.parse("/user/search/q;name=j");
 * PathContainer p2 = SimplePathContainer.parse("/user/search/q;name=j;strategy=startsWith");
 * }</pre>
 */
public class SimplePathContainer implements PathContainer {

    /** 🔗 Key/value delimiter inside matrix parameters (e.g. {@code name=j}). */
    public static final  char                            KEY_VALUE_SEPARATOR         = '=';
    /** 📎 Matrix parameter separator inside a segment (e.g. {@code ;name=j;sort=asc}). */
    public static final  char                            MATRIX_PARAMETERS_SEPARATOR = ';';
    /** 🫙 Shared empty container instance. */
    private static final PathContainer                   EMPTY_PATH                  = new SimplePathContainer("",
                                                                                                               emptyList());
    /** 🧱 Prebuilt separator elements for common separators. */
    private static final Map<Character, SimpleSeparator> SEPARATORS                  = new HashMap<>(2);

    static {
        SEPARATORS.put('/', new SimpleSeparator("/"));
        SEPARATORS.put('.', new SimpleSeparator("."));
    }

    /** 📜 Original path string. */
    private final String        path;
    /** 🧩 Parsed elements (separators + segments with params). */
    private final List<Element> elements;

    /**
     * 🏗️ Create a container from raw path and parsed elements.
     */
    public SimplePathContainer(String path, List<Element> elements) {
        this.path = path;
        this.elements = elements;
    }

    public static void main(String[] args) {
        SimplePathContainer.parse("/user/search/q;name=j;strategy=startsWith/");
    }

    /**
     * 📥 Parse using default options ({@link Options#defaults()}).
     *
     * @param path raw path string
     * @return parsed {@link PathContainer}
     */
    public static PathContainer parse(String path) {
        return parse(path, Options.defaults());
    }

    /**
     * 🔧 Parse using provided {@link Options}.
     *
     * <p>Produces a sequence of {@link Separator} and {@link PathElement}
     * preserving leading/trailing separators and segment boundaries.</p>
     *
     * @param path    raw path string
     * @param options parsing options (separator, matrix params parsing)
     * @return parsed {@link PathContainer}
     */
    public static PathContainer parse(String path, Options options) {
        char      defaultSeparator = options.separator();
        Separator separatorElement = SEPARATORS.get(defaultSeparator);

        if (!path.isEmpty()) {
            List<Element> elements          = new ArrayList<>();
            int           begin             = path.charAt(0) == defaultSeparator ? 1 : 0;
            boolean       endsWithSeparator = path.charAt(path.length() - 1) == defaultSeparator;
            String[]      segments          = StringHelper.tokenize(path, defaultSeparator);
            int           counter           = 0;

            if (begin == 1) {
                elements.add(separatorElement);
            }

            for (String segment : StringHelper.tokenize(path, defaultSeparator)) {
                counter++;
                elements.add(parsePath(segment, options));
                if (counter < segments.length || endsWithSeparator) {
                    elements.add(separatorElement);
                }
            }

            return new SimplePathContainer(path, elements);
        }

        return EMPTY_PATH;
    }

    /**
     * 🧩 Parse a single segment into {@link PathElement}, optionally
     * extracting matrix parameters (e.g. {@code q;name=j;sort=asc}).
     *
     * @param segment raw segment (no leading/trailing separator)
     * @param options parsing options
     * @return {@link PathElement} with path and parameters
     */
    public static PathElement parsePath(String segment, Options options) {
        Map<String, List<String>> parameters = new LinkedHashMap<>();
        int                       index      = segment.indexOf(MATRIX_PARAMETERS_SEPARATOR);
        String                    route      = segment;

        if (options.parseParameters() && segment.indexOf(MATRIX_PARAMETERS_SEPARATOR) != -1) {
            route = segment.substring(0, index);
            parseParameters(segment.substring(index + 1), parameters);
        }

        return new SimplePathElement(route, parameters);
    }

    /**
     * 🧷 Parse a parameters parameters tail (without the leading {@code ;})
     * into a multi-value map.
     *
     * @param segment parameters tail (e.g. {@code name=j;sort=asc})
     * @param parameters  output map to populate
     */
    public static void parseParameters(String segment, Map<String, List<String>> parameters) {
        int begin = 0;

        while (begin < segment.length()) {
            int    ending   = segment.indexOf(MATRIX_PARAMETERS_SEPARATOR, begin);
            String keyValue = (ending == -1) ? segment.substring(begin) : segment.substring(begin, ending);

            parseKeyValue(keyValue, parameters);

            if (ending == -1) {
                break;
            }

            begin = ending + 1;
        }
    }

    /**
     * 🔑 Parse a single {@code key[=value]} pair and store it in the map.
     *
     * <p>If value is absent, an empty string is stored for the key.</p>
     *
     * @param segment single {@code key} or {@code key=value}
     * @param parameters  output map to populate (multi-value)
     */
    public static void parseKeyValue(String segment, Map<String, List<String>> parameters) {
        if (segment != null && !segment.isEmpty()) {
            int    index = segment.indexOf(KEY_VALUE_SEPARATOR);
            String key   = segment;
            String value = "";

            if (index != -1) {
                key = segment.substring(0, index);
                value = segment.substring(index + 1);
            }

            parameters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
    }

    /**
     * 📚 Return parsed path elements in order (immutable view).
     */
    @Override
    public List<Element> elements() {
        return elements;
    }

    /**
     * 📦 Segment element: the literal path part plus its matrix parameters.
     *
     * <p>Example: for {@code "q;name=j;sort=asc"} → path: {@code "q"},
     * params: {@code {name:[j], sort:[asc]}}.</p>
     */
    public record SimplePathElement(String path, Map<String, List<String>> parameters) implements PathElement {

        /**
         * @return the literal path part of this segment (without parameters)
         */
        @Override
        public String value() {
            return path();
        }
    }

    /**
     * ➖ Separator element (e.g. {@code "/"} or {@code "."}).
     */
    public record SimpleSeparator(String separator) implements Separator {

        /**
         * @return the separator string as-is
         */
        @Override
        public String value() {
            return separator();
        }
    }
}
