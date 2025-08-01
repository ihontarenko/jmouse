package org.jmouse.mvc;

import org.jmouse.web.request.http.HttpMethod;

import java.util.*;

/**
 * 📦 Represents a full route definition: HTTP method(s) + path pattern.
 * <p>
 * Immutable, reusable and safe for use in routing maps.
 *
 * <pre>{@code
 * Route route = Route.of(HttpMethod.GET, "/users/{id:\\d+}");
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Route {

    private final Set<HttpMethod> methods;
    private final PathPattern     path;

    private Route(Set<HttpMethod> methods, PathPattern path) {
        this.methods = Set.copyOf(methods);
        this.path = path;
    }

    /**
     * ✨ Create a new route builder.
     *
     * @return new {@link Builder}
     */
    public static Builder route() {
        return new Builder();
    }

    /**
     * 🔧 Shorthand builder for a single-method route.
     *
     * @param method HTTP method
     * @param path   route path pattern
     * @return built {@link Route}
     */
    public static Route of(HttpMethod method, String path) {
        return new Builder().method(method).path(path).build();
    }

    /**
     * @return supported HTTP methods
     */
    public Set<HttpMethod> methods() {
        return methods;
    }

    /**
     * @return path pattern of the route
     */
    public PathPattern path() {
        return path;
    }

    @Override
    public String toString() {
        return "Route: %s: %s".formatted(methods, path);
    }

    @Override
    public boolean equals(Object that) {
        if (this == that)
            return true;

        if (!(that instanceof Route route))
            return false;

        return Objects.equals(methods, route.methods) && Objects.equals(path, route.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methods, path);
    }

    /**
     * 🏗️ Fluent route builder.
     *
     * <pre>{@code
     * Route route = Route.route()
     *     .GET("/api/items")
     *     .method(HttpMethod.HEAD)
     *     .build();
     * }</pre>
     */
    public static class Builder {

        private final Set<HttpMethod> methods = new LinkedHashSet<>();
        private       PathPattern     path;

        /**
         * Shorthand for GET route.
         *
         * @param path URI pattern
         * @return this builder
         */
        public Builder GET(String path) {
            return method(HttpMethod.GET).path(path);
        }

        /**
         * Shorthand for POST route.
         *
         * @param path URI pattern
         * @return this builder
         */
        public Builder POST(String path) {
            return method(HttpMethod.POST).path(path);
        }

        /**
         * Shorthand for PUT route.
         *
         * @param path URI pattern
         * @return this builder
         */
        public Builder PUT(String path) {
            return method(HttpMethod.PUT).path(path);
        }

        /**
         * Shorthand for PATCH route.
         *
         * @param path URI pattern
         * @return this builder
         */
        public Builder PATCH(String path) {
            return method(HttpMethod.PATCH).path(path);
        }

        /**
         * Shorthand for DELETE route.
         *
         * @param path URI pattern
         * @return this builder
         */
        public Builder DELETE(String path) {
            return method(HttpMethod.DELETE).path(path);
        }

        /**
         * Add a single HTTP method.
         *
         * @param method method to support
         * @return this builder
         */
        public Builder method(HttpMethod method) {
            this.methods.add(method);
            return this;
        }

        /**
         * Add multiple HTTP methods.
         *
         * @param methods methods to support
         * @return this builder
         */
        public Builder methods(HttpMethod... methods) {
            this.methods.addAll(List.of(methods));
            return this;
        }

        /**
         * Set the path pattern.
         *
         * @param path URI pattern
         * @return this builder
         */
        public Builder path(String path) {
            this.path = new PathPattern(path);
            return this;
        }

        /**
         * 🛠️ Finalize route construction.
         *
         * @return built {@link Route}
         * @throws IllegalStateException if method(s) or path is missing
         */
        public Route build() {
            if (methods.isEmpty()) {
                throw new IllegalStateException("At least one method is required");
            }

            if (path == null) {
                throw new IllegalStateException("Path is required");
            }

            return new Route(methods, path);
        }
    }

}
