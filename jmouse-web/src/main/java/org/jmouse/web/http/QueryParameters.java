package org.jmouse.web.http;

import java.util.*;

/**
 * 📦 Immutable wrapper for HTTP query parameters.
 * <p>
 * Converts a {@code Map<String, String[]>} (typically from {@code HttpServletRequest#getParameterMap()})
 * into a more convenient structure.
 * </p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @since 1.0
 */
public final class QueryParameters {

    public static final String QUERY_PARAMETERS_ATTRIBUTE = QueryParameters.class.getName() + ".QUERY_PARAMETERS";

    private final Map<String, List<String>> parameters;

    /**
     * Constructs a {@code QueryParameters} from raw {@code Map<String, String[]>}.
     *
     * @param source the original map of query parameters
     */
    private QueryParameters(Map<String, String[]> source) {
        parameters = new LinkedHashMap<>();
        source.forEach((parameter, values)
                -> parameters.put(parameter, List.of(values)));
    }

    /**
     * ✅ Factory method for creating {@code QueryParameters}.
     *
     * @param source the raw parameter map (usually from servlet API)
     * @return a new {@code QueryParameters} instance
     */
    public static QueryParameters ofMap(Map<String, String[]> source) {
        return new QueryParameters(source);
    }

    /**
     * 📚 Returns all values associated with the given parameter key.
     *
     * @param key the query parameter name
     * @return list of all values, or empty list if none found
     */
    public List<String> getAll(String key) {
        return parameters.getOrDefault(key, Collections.emptyList());
    }

    /**
     * 🔍 Returns the first value of a given parameter key.
     *
     * @param key the query parameter name
     * @return the first value, or {@code null} if none found
     */
    public String getFirst(String key) {
        List<String> values = getAll(key);

        if (values.isEmpty()) {
            return null;
        }

        return values.getFirst();
    }

    /**
     * ❓ Checks if the query contains the given parameter key.
     *
     * @param key the parameter name
     * @return {@code true} if present, {@code false} otherwise
     */
    public boolean contains(String key) {
        return parameters.containsKey(key);
    }

    /**
     * 🗝 Returns all parameter names.
     *
     * @return set of parameter keys
     */
    public Set<String> keys() {
        return parameters.keySet();
    }

    /**
     * 🧺 Checks if the parameter map is empty.
     *
     * @return {@code true} if no parameters are present
     */
    public boolean isEmpty() {
        return parameters.isEmpty();
    }

    /**
     * 🧾 Returns the internal map as an unmodifiable view.
     *
     * @return an unmodifiable map of parameters
     */
    public Map<String, List<String>> asMap() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public String toString() {
        return "Parameters" + parameters;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof QueryParameters that && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters);
    }
}
