package org.jmouse.web.http.request;

import java.util.*;

/**
 * 🔒 Value-object for the HTTP {@code If-Match} header.
 *
 * <p>Semantics are defined in
 * <a href="https://datatracker.ietf.org/doc/html/rfc9110#field.if-match">RFC 9110 §12.1.1</a>
 * (previously RFC 7232 §3.1).</p>
 *
 * <h3>Supported forms:</h3>
 * <ul>
 *   <li>{@code "*"} → resource must exist (server checks only for existence)</li>
 *   <li>{@code "etag-list"} → at least one strong match against the current resource ETag</li>
 * </ul>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * IfMatch ifMatch = IfMatch.parse("*"); // special ANY form
 * boolean allowed = ifMatch.matches(currentEtag, false, resourceExists);
 * }</pre>
 *
 * @see ETag
 * @see IfNoneMatch
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9110#field.if-match">RFC 9110 §12.1.1</a>
 */
public class IfMatch {

    /**
     * Whether this header is in {@code "*"} form.
     */
    private final boolean any;

    /**
     * Collection of entity tags, or empty if none.
     */
    private final Collection<ETag> eTags;

    protected IfMatch(boolean any, Collection<ETag> eTags) {
        this.any = any;
        this.eTags = toStableList(eTags);
    }

    /**
     * Create the wildcard form: {@code If-Match: *}.
     *
     * <p>Indicates that the request should only succeed if
     * the resource exists, regardless of its current ETag.</p>
     */
    public static IfMatch any() {
        return new IfMatch(true, List.of());
    }

    /**
     * Create an empty instance (header missing or unparsable).
     */
    public static IfMatch empty() {
        return new IfMatch(false, List.of());
    }

    /**
     * Create an {@code If-Match} from a collection of {@link ETag}s.
     *
     * @param eTags the tags to include
     * @return an instance with the given tags
     */
    public static IfMatch of(Collection<ETag> eTags) {
        return new IfMatch(false, Set.copyOf(eTags));
    }

    /**
     * Varargs convenience factory.
     */
    public static IfMatch of(ETag... eTag) {
        return of(List.of(eTag));
    }

    /**
     * Parse a raw string value into an {@code If-Match}.
     *
     * @param eTag raw header value
     * @return parsed instance
     */
    public static IfMatch of(String eTag) {
        return parse(eTag);
    }

    /**
     * Parse a header string into {@code If-Match}.
     *
     * <ul>
     *   <li>{@code "*"} → wildcard form</li>
     *   <li>Otherwise → parsed as list of {@link ETag}s</li>
     * </ul>
     *
     * @param ifMatch raw header value
     * @return parsed instance
     */
    public static IfMatch parse(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return empty();
        }

        ifMatch = ifMatch.trim();

        if ("*".equals(ifMatch)) {
            return any();
        }

        return of(ETag.parseAll(ifMatch));
    }

    /**
     * @return {@code true} if wildcard {@code *} form
     */
    public boolean isAny() {
        return any;
    }

    /**
     * @return {@code true} if empty and not wildcard
     */
    public boolean isEmpty() {
        return !any && eTags.isEmpty();
    }

    /**
     * Get the immutable list of entity tags.
     */
    public List<ETag> getETags() {
        return List.copyOf(eTags);
    }

    /**
     * Test whether the current resource state matches this {@code If-Match}.
     *
     * @param current   current resource {@link ETag} (nullable)
     * @param allowWeak whether weak comparison is acceptable
     * @param allowAny    whether the resource exists
     * @return {@code true} if the precondition is satisfied
     * @see ETag#strongEquals(ETag)
     * @see ETag#weakEquals(ETag)
     */
    public boolean matches(ETag current, boolean allowWeak, boolean allowAny) {
        if (any) {
            return allowAny;
        }

        if (current == null || eTags.isEmpty()) {
            return false;
        }

        for (ETag eTag : eTags) {
            if (allowWeak ? current.weakEquals(eTag) : current.strongEquals(eTag)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Alias for {@link #matches(ETag, boolean, boolean)}.
     */
    public boolean isSatisfied(ETag current, boolean allowWeak, boolean exists) {
        return matches(current, allowWeak, exists);
    }

    /**
     * Convert this instance into an {@link IfNoneMatch}.
     *
     * @return equivalent none-match representation
     */
    public IfNoneMatch toNoneMatch() {
        return new IfNoneMatch(any, eTags);
    }

    /**
     * 🔄 Normalize a collection of {@link ETag}s into a stable, unmodifiable list.
     *
     * <p>Ensures predictable iteration order and removes duplicates by using
     * a {@link LinkedHashSet}, which preserves insertion order while enforcing uniqueness.</p>
     *
     * <p>This is useful when parsing header values, where duplicates are invalid
     * but order should be stable for debugging and logging.</p>
     *
     * @param collection input collection (nullable/empty allowed)
     * @return immutable list with unique entries, never {@code null}
     */
    private static List<ETag> toStableList(Collection<ETag> collection) {
        if (collection == null || collection.isEmpty()) {
            return List.of();
        }

        Set<ETag> unique = new LinkedHashSet<>(collection);

        return List.copyOf(unique);
    }

    @Override
    public String toString() {
        if (isAny()) {
            return "If-Match[*]";
        }

        return "If-Match" + getETags();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof IfMatch that)) {
            return false;
        }

        return any == that.any && eTags.equals(that.eTags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(any, eTags);
    }
}
