package org.jmouse.web.http.request;

import org.jmouse.util.StringHelper;
import org.jmouse.web.http.HttpMethod;

import java.util.*;

/**
 * 📑 Representation of the HTTP {@code Allow} response header.
 *
 * <p>The {@code Allow} header indicates the set of HTTP methods
 * supported by a resource (e.g. in response to {@code OPTIONS}).</p>
 *
 * <ul>
 *   <li>{@code Allow: GET, POST} → only GET and POST are permitted</li>
 *   <li>{@code Allow: } (empty) → no methods permitted</li>
 *   <li>{@code Allow.any()} → all defined methods are permitted</li>
 * </ul>
 *
 * <p>Instances are immutable and provide idempotent merge operations.</p>
 *
 * @see <a href="https://developer.mozilla.org/docs/Web/HTTP/Headers/Allow">MDN: Allow header</a>
 */
public final class Allow {

    private final Set<HttpMethod> allow;

    public Allow(Set<HttpMethod> allow) {
        this.allow = allow;
    }

    /**
     * ✳️ Allow all supported methods.
     *
     * @return instance representing all methods
     */
    public static Allow any() {
        return of(HttpMethod.values());
    }

    /**
     * ➖ Empty allow set (no methods).
     *
     * @return instance with no methods
     */
    public static Allow empty() {
        return of(List.of());
    }

    /**
     * Parse from a raw comma-separated string.
     *
     * @param methods header string (e.g. {@code "GET, POST"})
     * @return parsed allow instance
     */
    public static Allow of(String methods) {
        if (methods == null || methods.isEmpty()) {
            return empty();
        }

        String[] tokens = StringHelper.tokenize(methods, ",");
        Set<HttpMethod> allow = EnumSet.noneOf(HttpMethod.class);

        for (String token : tokens) {
            allow.add(HttpMethod.ofName(token.trim()));
        }

        return of(allow);
    }

    /**
     * Create from varargs of methods.
     */
    public static Allow of(HttpMethod... methods) {
        return of(List.of(methods));
    }

    /**
     * Create from a collection of methods.
     */
    public static Allow of(Collection<HttpMethod> methods) {
        return new Allow(Set.copyOf(methods));
    }

    /**
     * @return {@code true} if no methods are allowed
     */
    public boolean isEmpty() {
        return allow.isEmpty();
    }

    /**
     * @return {@code true} if all {@link HttpMethod} values are allowed
     */
    public boolean isAny() {
        return allow.size() == HttpMethod.values().length;
    }

    /**
     * Check if this set allows the given method.
     *
     * @param method method to test
     * @return {@code true} if contained
     */
    public boolean contains(HttpMethod method) {
        return allow.contains(method);
    }

    /**
     * 🔗 Merge with another {@code Allow}.
     *
     * <p>Union of method sets; "any" dominates.</p>
     *
     * @param that other allow instance
     * @return new combined allow instance
     */
    public Allow merge(Allow that) {
        if (this.isAny() || that.isAny()) {
            return any();
        }
        if (that.allow.isEmpty()) {
            return this;
        }
        if (this.allow.isEmpty()) {
            return that;
        }

        Set<HttpMethod> allow = new HashSet<>(this.allow);
        allow.addAll(that.allow);

        return of(allow);
    }

    /**
     * Renders the allowed methods as a single HTTP header-line value,
     * e.g. {@code "GET, POST"}.
     *
     * <p><em>Note:</em> If no methods are present, this returns an empty string.
     * Callers should avoid emitting an {@code Allow} header with an empty value.</p>
     *
     * @return comma-separated list of HTTP methods, or an empty string if none
     * @see org.jmouse.web.http.HttpHeader#ALLOW
     */
    public String toHeaderValue() {
        if (allow.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        allow.stream().map(HttpMethod::name).forEach(joiner::add);
        return joiner.toString();
    }

    /**
     * Returns the allowed methods as an array for programmatic checks.
     *
     * @return array of allowed {@link HttpMethod}; never {@code null}
     */
    public HttpMethod[] toSupportedMethods() {
        return allow.toArray(new HttpMethod[0]);
    }

    @Override
    public String toString() {
        return "Allow[%s]".formatted(toHeaderValue());
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Allow allow)) {
            return false;
        }
        return this.allow.equals(allow.allow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allow);
    }
}
