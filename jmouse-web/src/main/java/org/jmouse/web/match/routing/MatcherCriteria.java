package org.jmouse.web.match.routing;

import org.jmouse.core.MediaType;
import org.jmouse.core.Streamable;
import org.jmouse.core.matcher.Matcher;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.match.PathPatternCompiler;
import org.jmouse.web.match.routing.condition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
public class MatcherCriteria implements MappingMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatcherCriteria.class);

    private final List<Matcher<RequestRoute>> matchers = new ArrayList<>();

    /**
     * 🔁 Replaces an existing matcher of the same type with the given one.
     * Ensures only one matcher of a specific class exists at a time.
     *
     * @param matcher matcher instance to register
     * @return this criteria instance for chaining
     */
    public MatcherCriteria set(Matcher<RequestRoute> matcher) {
        matchers.removeIf(m -> matcher.getClass().isInstance(m));
        return add(matcher);
    }

    /**
     * ➕ Adds a new matcher to the internal collection.
     *
     * @param matcher matcher instance to add
     * @return this criteria instance for chaining
     */
    public MatcherCriteria add(Matcher<RequestRoute> matcher) {
        matchers.add(matcher);
        return this;
    }

    /**
     * 🧩 Registers a path pattern matcher.
     *
     * @param path path expression (supports wildcards or templates)
     * @return this criteria instance for chaining
     */
    public MatcherCriteria pathPattern(String path) {
        return set(new RequestPathMatcher(PathPatternCompiler.compile(path)));
    }

    /**
     * ⚙️ Registers an HTTP method matcher by name.
     *
     * @param method HTTP method name (e.g. "GET", "POST")
     * @return this criteria instance for chaining
     */
    public MatcherCriteria httpMethod(String method) {
        return httpMethod(HttpMethod.ofName(method));
    }

    /**
     * ⚙️ Registers an HTTP method matcher.
     *
     * @param method {@link HttpMethod} to match
     * @return this criteria instance for chaining
     */
    public MatcherCriteria httpMethod(HttpMethod method) {
        return add(new HttpMethodMatcher(method));
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

    /**
     * ✅ Evaluates this matcher chain against the provided {@link RequestRoute}.
     *
     * @param route current request route
     * @return {@code true} if all matchers match; {@code false} otherwise
     */
    @Override
    public boolean matches(RequestRoute route) {
        Optional<Matcher<RequestRoute>> matcher = Streamable.of(matchers)
                .reduce(Matcher::logicalAnd);

        LOGGER.debug("MatcherCriteria: {} will be proceed!", matcher);

        return matcher.orElseGet(() -> Matcher.constant(true))
                .matches(route);
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
    public int compare(MappingMatcher other, RequestRoute requestRoute) {
        if (other instanceof MatcherCriteria that) {
            int result = Integer.compare(this.matchers.size(), that.matchers.size());
            if (result != 0) {
                return result;
            }

            for (int i = 0; i < this.matchers.size(); i++) {
                MappingMatcher matcherA = (MappingMatcher) this.matchers.get(i);
                MappingMatcher matcherB = (MappingMatcher) that.matchers.get(i);
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
