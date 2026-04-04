package org.jmouse.web.match.routing;

import org.jmouse.core.MediaType;
import org.jmouse.core.matcher.Match;
import org.jmouse.core.matcher.MatchOp;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.PathPattern;
import org.jmouse.web.match.PathPatternCompiler;
import org.jmouse.web.match.routing.condition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.jmouse.core.Streamable.of;

/**
 * 🧭 Represents a composite matcher used to evaluate whether a given
 * {@link RequestRoute} satisfies a set of matching conditions such as
 * HTTP method, path, headers, query parameters, and media types.
 *
 * <p>Each criterion is represented by a {@link Matcher} and combined
 * logically using {@code AND} semantics — meaning all registered matchers
 * must match for the route to be considered valid.</p>
 *
 * <p>✨ <b>Highlights:</b></p>
 * <ul>
 *     <li>Composes multiple {@link Matcher} instances.</li>
 *     <li>Supports path, method, headers, query, consumes, and produces matchers.</li>
 *     <li>Provides {@link #compare(MappingMatcher, RequestRoute)} for matcher ordering.</li>
 * </ul>
 *
 * @see Matcher
 * @see RequestRoute
 * @see MappingMatcher
 */
public class MatcherCriteria implements MappingMatcher<RequestRoute> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatcherCriteria.class);

    protected final List<MappingMatcher<RequestRoute>> matchers = new ArrayList<>();

    /**
     * ✅ Creates a {@link MatcherCriteria} instance that always matches any route.
     *
     * <p>Internally registers an {@link AnyCondition} matcher, which unconditionally
     * returns {@code true} for any {@link org.jmouse.web.http.RequestRoute}.</p>
     *
     * <p>Useful as a universal fallback or default route matcher.</p>
     *
     * @return a {@link MatcherCriteria} that matches all routes
     * @see AnyCondition
     */
    public static MatcherCriteria any() {
        return new MatcherCriteria().add(AnyCondition.INSTANCE);
    }

    /**
     * 🚫 Creates a {@link MatcherCriteria} instance that never matches any route.
     *
     * <p>Internally registers a {@link NoneCondition} matcher, which always
     * returns {@code false} for any {@link org.jmouse.web.http.RequestRoute}.</p>
     *
     * <p>Useful as a placeholder or for negative test conditions.</p>
     *
     * @return a {@link MatcherCriteria} that matches no routes
     * @see NoneCondition
     */
    public static MatcherCriteria none() {
        return new MatcherCriteria().add(NoneCondition.INSTANCE);
    }

    /**
     * 🔁 Replaces an existing matcher of the same type with the given one.
     * Ensures only one matcher of a specific class exists at a time.
     *
     * @param matcher matcher instance to register
     * @return this criteria instance for chaining
     */
    public MatcherCriteria set(MappingMatcher<RequestRoute> matcher) {
        matchers.removeIf(m -> matcher.getClass().isInstance(m));
        return add(matcher);
    }

    /**
     * ➕ Adds a new matcher to the internal collection.
     *
     * @param matcher matcher instance to add
     * @return this criteria instance for chaining
     */
    public MatcherCriteria add(MappingMatcher<RequestRoute> matcher) {
        matchers.add(matcher);
        return this;
    }

    /**
     * 🧩 Registers a path pattern matcher.
     *
     * <p>Compiles the given raw path expression into a {@link PathPattern}
     * and registers it as a matcher for incoming {@link RequestRoute}s.</p>
     *
     * <p>Supports standard template syntax such as:
     * <pre>{@code
     * /api/users/{id}
     * /assets/**
     * }</pre>
     *
     * @param path raw path expression (supports wildcards or templates)
     * @return this criteria instance for chaining
     * @see #pathPattern(PathPattern)
     */
    public MatcherCriteria pathPattern(String path) {
        return pathPattern(PathPatternCompiler.compile(path));
    }

    /**
     * 🧭 Registers a path pattern matcher using a precompiled {@link PathPattern}.
     *
     * <p>This overload is useful when patterns are pre-parsed or cached
     * for efficiency, allowing reuse without recompilation.</p>
     *
     * @param pathPattern compiled {@link PathPattern} to match against
     * @return this criteria instance for chaining
     */
    public MatcherCriteria pathPattern(PathPattern pathPattern) {
        return set(new RequestPathMatcher(pathPattern));
    }

    /**
     * 🧭 Registers a context-path matcher.
     *
     * <p>Compiles the given raw context path expression into a {@link PathPattern}
     * and registers it as a matcher for incoming {@link RequestRoute}s.</p>
     *
     * <p>Unlike {@link #pathPattern(String)}, this matcher is evaluated against
     * {@code requestPath().contextPath()}, making it suitable for applications
     * deployed under a specific base path (e.g. {@code "/api"}).</p>
     *
     * <p>Supports standard template syntax such as:
     * <pre>{@code
     * /api
     * /tenant/{tenantId}
     * }</pre>
     *
     * @param path raw context path expression (supports templates)
     * @return this criteria instance for chaining
     * @see #contextPattern(PathPattern)
     */
    public MatcherCriteria contextPattern(String path) {
        return contextPattern(PathPatternCompiler.compile(path));
    }

    /**
     * 🧩 Registers a context-path matcher using a precompiled {@link PathPattern}.
     *
     * <p>This overload is useful when context path patterns are pre-parsed or cached
     * for reuse, avoiding repeated compilation.</p>
     *
     * <p>The matcher evaluates {@code requestPath().contextPath()} and attaches
     * matching facets via {@link ContextPathMatcher}.</p>
     *
     * @param pathPattern compiled {@link PathPattern} to match against context path
     * @return this criteria instance for chaining
     */
    public MatcherCriteria contextPattern(PathPattern pathPattern) {
        return set(new ContextPathMatcher(pathPattern));
    }

    /**
     * 🚏 Registers a servlet-path matcher.
     *
     * <p>Compiles the given raw servlet path expression into a {@link PathPattern}
     * and registers it as a matcher for incoming {@link RequestRoute}s.</p>
     *
     * <p>Unlike {@link #pathPattern(String)}, this matcher is evaluated against
     * {@code requestPath().servletPath()}, making it suitable for matching the
     * request path within the current servlet mapping.</p>
     *
     * <p>Supports standard template syntax such as:
     * <pre>{@code
     * /users/{id}
     * /assets/**
     * }</pre>
     *
     * @param path raw servlet path expression (supports wildcards or templates)
     * @return this criteria instance for chaining
     * @see #servletPattern(PathPattern)
     */
    public MatcherCriteria servletPattern(String path) {
        return servletPattern(PathPatternCompiler.compile(path));
    }

    /**
     * 🧩 Registers a servlet-path matcher using a precompiled {@link PathPattern}.
     *
     * <p>This overload is useful when servlet path patterns are pre-parsed or cached
     * for reuse, avoiding repeated compilation.</p>
     *
     * <p>The matcher evaluates {@code requestPath().servletPath()} and attaches
     * matching facets via {@link ServletPathMatcher}.</p>
     *
     * @param pathPattern compiled {@link PathPattern} to match against servlet path
     * @return this criteria instance for chaining
     */
    public MatcherCriteria servletPattern(PathPattern pathPattern) {
        return set(new ServletPathMatcher(pathPattern));
    }

    /**
     * ⚙️ Registers an HTTP method matcher by name.
     *
     * @param methods HTTP method names (e.g. "GET", "POST")
     * @return this criteria instance for chaining
     */
    public MatcherCriteria httpMethod(String... methods) {
        return httpMethod(of(methods).map(HttpMethod::valueOf).toArray(HttpMethod[]::new));
    }

    /**
     * ⚙️ Registers an HTTP method matcher.
     *
     * @param methods {@link HttpMethod}s to match
     * @return this criteria instance for chaining
     */
    public MatcherCriteria httpMethod(HttpMethod... methods) {
        return set(new HttpMethodMatcher(methods));
    }

    /**
     * 🧾 Registers a header matcher by name.
     *
     * @param header header name
     * @param value  expected header value
     * @return this criteria instance for chaining
     */
    public MatcherCriteria header(String header, Object value) {
        return header(HttpHeader.ofHeader(header), value);
    }

    /**
     * 🧾 Registers a header matcher using a {@link HttpHeader} constant.
     *
     * @param header {@link HttpHeader} instance
     * @param value  expected value
     * @return this criteria instance for chaining
     */
    public MatcherCriteria header(HttpHeader header, Object value) {
        return add(new HttpHeaderMatcher(header, value));
    }

    /**
     * 🔍 Registers a query parameter matcher.
     *
     * @param name  parameter name
     * @param value expected value
     * @return this criteria instance for chaining
     */
    public MatcherCriteria query(String name, Object value) {
        return add(new QueryParameterMatcher(name, value));
    }

    /**
     * 🍽️ Registers a "consumes" media type matcher.
     *
     * @param mediaType accepted request content types
     * @return this criteria instance for chaining
     */
    public MatcherCriteria consumes(MediaType... mediaType) {
        return add(new ConsumesMatcher(Set.of(mediaType)));
    }

    /**
     * 🎨 Registers a "produces" media type matcher.
     *
     * @param mediaType expected response content types
     * @return this criteria instance for chaining
     */
    public MatcherCriteria produces(MediaType... mediaType) {
        return add(new ProducesMatcher(Set.of(mediaType)));
    }

    public static MatcherCriteria ANY(String pattern) {
        return new MatcherCriteria().pathPattern(pattern).httpMethod(HttpMethod.values());
    }

    public static MatcherCriteria GET(String pattern) {
        return new MatcherCriteria().pathPattern(pattern).httpMethod(HttpMethod.GET);
    }

    public static MatcherCriteria POST(String pattern) {
        return new MatcherCriteria().pathPattern(pattern).httpMethod(HttpMethod.POST);
    }

    public static MatcherCriteria PUT(String pattern) {
        return new MatcherCriteria().pathPattern(pattern).httpMethod(HttpMethod.PUT);
    }

    public static MatcherCriteria PATCH(String pattern) {
        return new MatcherCriteria().pathPattern(pattern).httpMethod(HttpMethod.PATCH);
    }

    public static MatcherCriteria DELETE(String pattern) {
        return new MatcherCriteria().pathPattern(pattern).httpMethod(HttpMethod.DELETE);
    }

    /**
     * 📋 Returns all registered matchers as an immutable list.
     *
     * @return an unmodifiable list of matchers
     */
    public List<Matcher<RequestRoute>> getMatchers() {
        return List.copyOf(matchers);
    }

    /**
     * 📂 Returns all matchers of a given type.
     *
     * @param type matcher class type
     * @param <T>  generic matcher subtype
     * @return list of matchers of the requested type
     */
    public <T extends Matcher<RequestRoute>> List<T> getMatchers(Class<T> type) {
        List<T> result = new ArrayList<>();

        for (Matcher<RequestRoute> candidate : getMatchers()) {
            if (type.isInstance(candidate)) {
                result.add(type.cast(candidate));
            }
        }

        return result;
    }

    /**
     * 🎯 Returns the first matcher of the given type.
     *
     * @param type matcher class type
     * @param <T>  generic matcher subtype
     * @return the first matcher instance found
     * @throws NoSuchElementException if no matcher of that type exists
     */
    public <T extends Matcher<RequestRoute>> T getMatcher(Class<T> type) {
        return getMatchers(type).getFirst();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Match apply(RequestRoute value) {
        List<Match> matches = new ArrayList<>();

        for (Matcher<RequestRoute> matcher : matchers) {
            if (matcher instanceof MappingMatcher<?> mappingMatcher) {
                MatchOp<RequestRoute, Match> matchOp = (MatchOp<RequestRoute, Match>) mappingMatcher;
                matches.add(matchOp.match(value));
            }
        }

        return Match.and(matches.toArray(Match[]::new));
    }

    /**
     * ⚖️ Compares two {@link MappingMatcher} instances for ordering purposes.
     * Used to prioritize more specific matchers.
     *
     * @param other        the other matcher to compare against
     * @param requestRoute current route context
     * @return comparison result (-1, 0, 1)
     */
    @Override
    public int compare(MappingMatcher<?> other, RequestRoute requestRoute) {
        if (other instanceof MatcherCriteria that) {
            int result = Integer.compare(this.matchers.size(), that.matchers.size());
            if (result != 0) {
                return result;
            }

            for (int i = 0; i < this.matchers.size(); i++) {
                MappingMatcher<RequestRoute> matcherA = this.matchers.get(i);
                MappingMatcher<RequestRoute> matcherB = that.matchers.get(i);
                result = matcherA.compare(matcherB, requestRoute);
                if (result != 0) {
                    return result;
                }
            }
        }
        return 0;
    }

    /** ♻️ Equality based on contained matchers. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof MatcherCriteria that)) {
            return false;
        }

        return Objects.equals(matchers, that.matchers);
    }

    /** 🧮 Hash code computed from all matchers. */
    @Override
    public int hashCode() {
        return Objects.hash(matchers);
    }

    /** 🪶 Human-readable representation for debugging. */
    @Override
    public String toString() {
        return "MatcherCriteria[" + matchers.size() + "]";
    }

}
