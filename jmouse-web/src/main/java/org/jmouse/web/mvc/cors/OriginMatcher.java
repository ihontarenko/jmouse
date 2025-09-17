package org.jmouse.web.mvc.cors;

import org.jmouse.core.matcher.Matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Matches request {@code Origin} values against an allow-list with simple wildcards and port lists.
 *
 * <p>Supported patterns:</p>
 * <ul>
 *   <li>Exact origins (case-insensitive), e.g. {@code "https://example.com"}</li>
 *   <li>Host wildcards via {@code *}, e.g. {@code "https://*.example.com"}</li>
 *   <li>Optional port wildcards or lists via suffix {@code :[...]}:
 *     <ul>
 *       <li>{@code :[*]} — any port (or no port)</li>
 *       <li>{@code :[80,443]} — explicit list of ports</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Note: Browsers never send {@code Origin: *}; if {@code "*"} is passed to
 * {@link #matches(String)}, it is treated as “allow all” for convenience.</p>
 */
class OriginMatcher implements Matcher<String> {

    /**
     * Base + optional port list in the form {@code base:[*|p1,p2,...]}.
     */
    private static final Pattern PORTS_PATTERN = compile("^(.*?)(?::\\[(\\*|[0-9,\\s]+)])?$");

    private final List<Pattern> patterns;
    private final List<String>  allowedOrigins;

    /**
     * Creates a matcher for the provided allow-list.
     *
     * @param allowedOrigins list of allowed origin patterns (must not be {@code null})
     */
    public OriginMatcher(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
        this.patterns = createPatterns(allowedOrigins);
    }

    /**
     * Checks whether the given {@code Origin} header value is allowed.
     *
     * <p>Order of checks:</p>
     * <ol>
     *   <li>Null or {@code "*"} → allowed.</li>
     *   <li>Exact (case-insensitive) match against the allow-list.</li>
     *   <li>Regex match against compiled wildcard/port patterns.</li>
     * </ol>
     *
     * @param origin origin header value (e.g., {@code "https://foo.example:443"})
     * @return {@code true} if allowed, {@code false} otherwise
     */
    @Override
    public boolean matches(String origin) {
        if (origin == null || origin.equals(WebCorsProcessor.WILDCARD)) {
            return true;
        }

        for (String allowedOrigin : allowedOrigins) {
            if (allowedOrigin.equalsIgnoreCase(origin)) {
                return true;
            }
        }

        for (Pattern pattern : patterns) {
            if (pattern.matcher(origin).matches()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Compiles wildcard/port patterns from the allow-list.
     *
     * <p>Implementation details:</p>
     * <ul>
     *   <li>The base is quoted with {@code \Q...\E} and {@code *} is expanded to {@code .*}.</li>
     *   <li>{@code :[*]} appends {@code (:\d+)?} (any or no port).</li>
     *   <li>{@code :[80,443]} trims spaces and appends {@code :(80|443)}.</li>
     * </ul>
     */
    private List<Pattern> createPatterns(List<String> origins) {
        List<Pattern> patterns = new ArrayList<>(origins.size());

        for (String value : origins) {
            String                  ports    = null;
            java.util.regex.Matcher matcher = PORTS_PATTERN.matcher(value);

            if (matcher.matches()) {
                value = matcher.group(1);
                ports  = matcher.group(2);
            }

            // Quote literal part, then re-enable regex for '*' wildcards
            String expression = "\\Q%s\\E".formatted(value).replace("*", "\\E.*\\Q");

            if (ports != null) {
                if (WebCorsProcessor.WILDCARD.equals(ports)) {
                    expression += "(:\\d+)?";
                } else {
                    String portsExpression = Arrays.stream(ports.split(","))
                            .map(String::trim)
                            .filter(port -> !port.isEmpty())
                            .reduce((a, b) -> a + "|" + b)
                            .orElse("");
                    expression += ":(" + portsExpression + ")";
                }
            }

            patterns.add(compile(expression, Pattern.CASE_INSENSITIVE));
        }

        return patterns;
    }
}
