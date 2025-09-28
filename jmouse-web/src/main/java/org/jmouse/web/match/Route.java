package org.jmouse.web.match;

import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;

import java.util.*;

/**
 * 🧭 Immutable route definition used for mapping requests to handlers.
 * <p>
 * Supports HTTP method, path matched, media types, headers, and query constraints.
 *
 * <p>Example usage:
 * <pre>{@code
 * Route route = Route.route()
 *     .GET("/users/{id:int}")
 *     .consumes(MediaType.APPLICATION_JSON)
 *     .produces(MediaType.APPLICATION_XML)
 *     .header(HttpHeader.ACCEPT_LANGUAGE, "en")
 *     .build();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Route {

    private final HttpMethod          method;
    private final PathPattern         path;
    private final Map<String, Object> queryParameters;
    private final Set<MediaType>      consumes;
    private final Set<MediaType>      produces;
    private final Headers             headers = new Headers();

    private Route(Builder builder) {
        this.method = builder.method;
        this.path = PathPatternCompiler.compile(builder.path);
        this.queryParameters = Map.copyOf(builder.queryParameters);
        this.consumes = Set.copyOf(builder.consumes);
        this.produces = Set.copyOf(builder.produces);
        this.headers.setAll(builder.headers.asMap());
    }

    /**
     * 🔧 Creates a basic route instance with method and path.
     *
     * @param method the HTTP method (e.g. GET, POST)
     * @param path   the route path matched (e.g. "/users/{id}")
     * @return new {@code Route} instance
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * Route.of(HttpMethod.PUT, "/api/items/{id}");
     * }</pre>
     */
    public static Route of(HttpMethod method, String path) {
        return route().method(method).path(path).build();
    }

    /**
     * 📥 Creates a GET route for the given path.
     *
     * @param path the route path matched (e.g. "/users")
     * @return new {@code Route} instance with GET method
     */
    public static Route GET(String path) {
        return route().GET(path).build();
    }

    /**
     * 📥 Creates a HEAD route for the given path.
     *
     * @param path the route path matched (e.g. "/users")
     * @return new {@code Route} instance with HEAD method
     */
    public static Route HEAD(String path) {
        return route().HEAD(path).build();
    }

    /**
     * 📥 Creates a OPTIONS route for the given path.
     *
     * @param path the route path matched (e.g. "/users")
     * @return new {@code Route} instance with OPTIONS method
     */
    public static Route OPTIONS(String path) {
        return route().OPTIONS(path).build();
    }

    /**
     * 📤 Creates a POST route for the given path.
     *
     * @param path the route path matched (e.g. "/submit")
     * @return new {@code Route} instance with POST method
     */
    public static Route POST(String path) {
        return route().POST(path).build();
    }

    /**
     * ✏️ Creates a PUT route for the given path.
     *
     * @param path the route path matched (e.g. "/resource/{id}")
     * @return new {@code Route} instance with PUT method
     */
    public static Route PUT(String path) {
        return route().PUT(path).build();
    }

    /**
     * 🔄 Creates a PATCH route for the given path.
     *
     * @param path the route path matched (e.g. "/data/{id}")
     * @return new {@code Route} instance with PATCH method
     */
    public static Route PATCH(String path) {
        return route().PATCH(path).build();
    }

    /**
     * ❌ Creates a DELETE route for the given path.
     *
     * @param path the route path matched (e.g. "/users/{id}")
     * @return new {@code Route} instance with DELETE method
     */
    public static Route DELETE(String path) {
        return route().DELETE(path).build();
    }

    /**
     * @return New route builder instance
     */
    public static Builder route() {
        return new Builder();
    }

    /**
     * @return HTTP method (GET, POST, etc.)
     */
    public HttpMethod httpMethod() {
        return method;
    }

    /**
     * @return Parsed path matched with variables and constraints
     */
    public PathPattern pathPattern() {
        return path;
    }

    /**
     * @return Query parameters matchers required for this route
     */
    public Map<String, Object> queryParameters() {
        return queryParameters;
    }

    /**
     * @return Accepted request body media types (e.g. JSON, XML)
     */
    public Set<MediaType> consumes() {
        return consumes;
    }

    /**
     * @return Supported response media types (e.g. JSON, HTML)
     */
    public Set<MediaType> produces() {
        return produces;
    }

    /**
     * @return Header matchers required for this route
     */
    public Headers headers() {
        return headers;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that)
            return true;

        if (!(that instanceof Route route))
            return false;

        return method == route.method &&
                Objects.equals(path, route.path) &&
                Objects.equals(queryParameters, route.queryParameters) &&
                Objects.equals(consumes, route.consumes) &&
                Objects.equals(produces, route.produces) &&
                Objects.equals(headers, route.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, queryParameters, consumes, produces, headers);
    }

    @Override
    public String toString() {
        return "Route[" +
                method + " " + (path == null ? "(Unknown)" : path.raw()) +
                (queryParameters.isEmpty() ? "" : ", queryParameters=" + queryParameters) +
                (consumes.isEmpty() ? "" : ", consumes=" + consumes) +
                (produces.isEmpty() ? "" : ", produces=" + produces) +
                (headers.isEmpty()  ? "" : ", headers="  + headers) +
                "]";
    }

    /**
     * 🛠 Builder for creating immutable {@link Route} instances.
     */
    public static final class Builder {

        private final Headers             headers         = new Headers();
        private final Set<MediaType>      consumes        = new LinkedHashSet<>();
        private final Set<MediaType>      produces        = new LinkedHashSet<>();
        private       HttpMethod          method;
        private       String              path;
        private final Map<String, Object> queryParameters = new LinkedHashMap<>();

        /**
         * Sets the HTTP method for the route.
         */
        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        /**
         * Shortcut for {@code method(HttpMethod.GET).path(path)}
         */
        public Builder GET(String path) {
            return method(HttpMethod.GET).path(path);
        }

        /**
         * Shortcut for {@code method(HttpMethod.HEAD).path(path)}
         */
        public Builder HEAD(String path) {
            return method(HttpMethod.HEAD).path(path);
        }

        /**
         * Shortcut for {@code method(HttpMethod.POST).path(path)}
         */
        public Builder POST(String path) {
            return method(HttpMethod.POST).path(path);
        }

        /**
         * Shortcut for {@code method(HttpMethod.PUT).path(path)}
         */
        public Builder PUT(String path) {
            return method(HttpMethod.PUT).path(path);
        }

        /**
         * Shortcut for {@code method(HttpMethod.DELETE).path(path)}
         */
        public Builder DELETE(String path) {
            return method(HttpMethod.DELETE).path(path);
        }

        /**
         * Shortcut for {@code method(HttpMethod.PATCH).path(path)}
         */
        public Builder PATCH(String path) {
            return method(HttpMethod.PATCH).path(path);
        }

        /**
         * Shortcut for {@code method(HttpMethod.OPTIONS).path(path)}
         */
        public Builder OPTIONS(String path) {
            return method(HttpMethod.OPTIONS).path(path);
        }

        /**
         * Sets the path matched, e.g. {@code /user/{id:int}}.
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets supported request content types.
         */
        public Builder consumes(MediaType... types) {
            Collections.addAll(this.consumes, types);
            return this;
        }

        /**
         * Sets supported response content types.
         */
        public Builder produces(MediaType... types) {
            Collections.addAll(this.produces, types);
            return this;
        }

        /**
         * Adds a header constraint for the route (e.g. {@code X-Custom: value}).
         */
        public Builder header(HttpHeader name, Object value) {
            this.headers.setHeader(name, value);
            return this;
        }

        /**
         * Adds a query parameter constraint for the route (e.g. {@code page=2}).
         */
        public Builder queryParameter(String name, Object value) {
            this.queryParameters.put(name, value);
            return this;
        }

        /**
         * Builds a new immutable {@link Route} instance.
         *
         * @throws IllegalStateException if method or path is missing
         */
        public Route build() {
            if (method == null || path == null) {
                throw new IllegalStateException("Both method and path are required");
            }

            return new Route(this);
        }

    }
}
