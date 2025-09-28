package org.jmouse.web.mvc.cors;

import org.jmouse.web.match.RegexpPathPattern;
import org.jmouse.web.match.SimplePathPattern;
import org.jmouse.web.match.AntPattern;
import org.jmouse.web.match.PathPattern;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 📦 Registry of CORS configurations keyed by {@link PathPattern}.
 *
 * <p>Preserves insertion order (via {@link LinkedHashMap}). Not thread-safe.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * CorsMappingRegistry registry = new CorsMappingRegistry();
 * registry.addMapping("/api/**")
 *         .allowedOrigins("https://example.com")
 *         .allowedMethods("GET", "POST");
 * }</pre>
 *
 * @see PathPattern
 * @see AntPattern
 * @see RegexpPathPattern
 * @see SimplePathPattern
 * @see CorsConfiguration
 */
public class CorsMappingRegistry {

    /**
     * Route pattern → CORS configuration.
     */
    private final Map<PathPattern, CorsConfiguration> mappings = new LinkedHashMap<>();

    /**
     * Registers a single {@link CorsConfiguration} instance for all given patterns.
     *
     * <p>If any pattern is already registered, an {@link IllegalStateException} is thrown and
     * no further patterns are processed.</p>
     *
     * @param patterns one or more Ant-style patterns (e.g., {@code "/api/**"})
     * @return the newly created configuration to be customized fluently
     * @throws IllegalStateException if a provided pattern is already mapped
     */
    public CorsConfiguration addMapping(String... patterns) {
        CorsConfiguration config = new CorsConfiguration();
        for (String pattern : patterns) {
            addMapping(new AntPattern(pattern), config);
        }
        return config;
    }

    /**
     * Registers the given {@link CorsConfiguration} for an {@link PathPattern}.
     *
     * @param antPattern    route pattern
     * @param configuration CORS configuration to associate
     * @throws IllegalStateException if the pattern is already registered
     */
    public void addMapping(PathPattern antPattern, CorsConfiguration configuration) {
        CorsConfiguration previous = mappings.get(antPattern);

        if (previous != null) {
            throw new IllegalStateException(
                    "AMBIGUOUS MAPPING! Cannot register CORS configuration for route '%s' there is already exists!".formatted(
                            antPattern));
        }

        mappings.put(antPattern, configuration);
    }

    /**
     * Returns the underlying mapping view.
     *
     * <p>Live view: modifications affect the registry. Consider exposing an
     * unmodifiable copy to callers if external mutation is undesirable.</p>
     *
     * @return map of {@link AntPattern} to {@link CorsConfiguration}
     */
    public Map<PathPattern, CorsConfiguration> getMappings() {
        return mappings;
    }

    /**
     * Looks up configuration by route path.
     *
     * @param path request URI
     * @return configuration or {@code null} if not registered
     */
    public CorsConfiguration lookup(String path) {
        CorsConfiguration configuration = null;

        for (Map.Entry<PathPattern, CorsConfiguration> entry : getMappings().entrySet()) {
            PathPattern routePath = entry.getKey();
            if (routePath.matches(path)) {
                configuration = entry.getValue();
                break;
            }
        }

        return configuration;
    }

    /**
     * Checks whether any registered pattern matches the given path.
     *
     * @param path request path to test (e.g., {@code "/api/users"})
     * @return {@code true} if at least one mapping matches; {@code false} otherwise
     */
    public boolean hasMapping(String path) {
        return mappings.keySet().stream().anyMatch(p -> p.matches(path));
    }
}
