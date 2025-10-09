package org.jmouse.web.http;

import java.util.Collection;

/**
 * 🔓 Value-object for the HTTP {@code If-None-Match} header.
 *
 * <p>Semantics are defined in
 * <a href="https://datatracker.ietf.org/doc/html/rfc9110#field.if-none-match">RFC 9110 §12.1.2</a>
 * (previously RFC 7232 §3.2).</p>
 *
 * <h3>Supported forms:</h3>
 * <ul>
 *   <li>{@code "*"} → matches any existing resource (i.e. request
 *       is allowed only if the resource does not exist)</li>
 *   <li>{@code "etag-list"} → request is allowed only if the current
 *       entity-tag <em>does not</em> match any in the list</li>
 * </ul>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * IfNoneMatch inm = new IfNoneMatch(true, List.of()); // ANY form
 * boolean allowed = inm.isSatisfied(currentEtag, false, resourceExists);
 * }</pre>
 *
 * @see ETag
 * @see IfMatch
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9110#field.if-none-match">RFC 9110 §12.1.2</a>
 */
public class IfNoneMatch extends IfMatch {

    /**
     * Protected constructor (use factories in {@link IfMatch}).
     *
     * @param any   whether this represents the wildcard form "*"
     * @param eTags collection of entity-tags (immutable copy is kept in super)
     */
    protected IfNoneMatch(boolean any, Collection<ETag> eTags) {
        super(any, eTags);
    }

    /**
     * Test whether the current resource state satisfies this {@code If-None-Match}.
     *
     * <p>Semantics: request is <em>allowed</em> if the resource's
     * current ETag <strong>does not match</strong> any provided ETag
     * (or, for the {@code "*"} form, if the resource does not exist).</p>
     *
     * @param current   current resource {@link ETag} (nullable)
     * @param allowWeak whether weak comparison is acceptable
     * @param exists    whether the resource exists
     * @return {@code true} if the precondition is satisfied
     *
     * @see IfMatch#matches(ETag, boolean, boolean)
     */
    @Override
    public boolean isSatisfied(ETag current, boolean allowWeak, boolean exists) {
        return !matches(current, allowWeak, exists);
    }

    @Override
    public String toString() {
        if (isAny()) {
            return "If-None-Match[*]";
        }
        return "If-None-Match" + getETags();
    }
}
