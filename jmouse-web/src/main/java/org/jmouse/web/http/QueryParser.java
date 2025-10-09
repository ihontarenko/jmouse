package org.jmouse.web.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🔎 Simple query string parser.
 *
 * <p>Converts raw {@code ?key=value&key2=value2} strings into
 * structured {@link QueryString} objects.</p>
 */
public class QueryParser {

    /**
     * 📥 Parse a raw query string into a {@link QueryString}.
     *
     * @param qs raw query string (without leading {@code ?})
     * @return parsed {@link QueryString} (never {@code null})
     */
    public static QueryString parse(String qs) {
        QueryString query = new QueryString();

        if (qs != null && !qs.isEmpty()) {
            int position = 0;
            while (qs.length() > position) {
                int    ampersand = qs.indexOf("&", position);
                String parameter = (ampersand < 0) ? qs.substring(position) : qs.substring(position, ampersand);
                int    equal     = parameter.indexOf("=");

                if (equal != -1) {
                    String key   = parameter.substring(0, equal);
                    String value = parameter.substring(equal + 1);
                    query.addParameter(key, value);
                }

                if (ampersand < 0) {
                    break;
                }

                position = ampersand + 1;
            }
        }

        return query;
    }

    /**
     * 📦 Represents a parsed query string as a map of parameters.
     */
    public static class QueryString {

        private final Map<String, QueryParameter> parameters = new HashMap<>();

        /**
         * ➕ Add a parameter with explicit {@link QueryParameter}.
         */
        public void addParameter(String name, QueryParameter parameter) {
            parameters.put(name, parameter);
        }

        /**
         * ➕ Add a parameter with a name and value.
         */
        public void addParameter(String name, String value) {
            parameters.put(name, new QueryParameter(name, value));
        }

        /**
         * ➕ Add a parameter record directly.
         */
        public void addParameter(QueryParameter parameter) {
            addParameter(parameter.name, parameter);
        }

        /**
         * 🔍 Get a parameter by name.
         *
         * @param name parameter name
         * @return the {@link QueryParameter} if present, otherwise {@code null}
         */
        public QueryParameter getParameter(String name) {
            return parameters.get(name);
        }

        /**
         * 🔄 Convert parameters back into a query string, excluding given keys.
         *
         * <p>Keys listed in {@code ignored} will not appear in the output.</p>
         *
         * @param keys parameter names to exclude
         * @return query string without leading {@code ?}
         */
        public String toQueryString(String... keys) {
            if (parameters.isEmpty()) {
                return "";
            }

            List<String> ignored = List.of(keys);
            StringBuilder builder = new StringBuilder();
            boolean first = true;

            for (QueryParameter parameter : parameters.values()) {
                if (ignored.contains(parameter.name)) {
                    continue;
                }

                if (!first) {
                    builder.append('&');
                }

                builder.append(parameter.name);

                if (!parameter.value.isEmpty()) {
                    builder.append('=').append(parameter.value());
                }

                first = false;
            }

            return builder.toString();
        }
    }

    /**
     * 🏷️ Single query parameter (name + value).
     *
     * @param name  parameter name
     * @param value parameter value
     */
    public record QueryParameter(String name, String value) {}
}
