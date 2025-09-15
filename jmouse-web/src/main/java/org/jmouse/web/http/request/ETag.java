package org.jmouse.web.http.request;

import org.jmouse.util.StringHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 🏷️ Immutable value object representing an HTTP {@code ETag}.
 *
 * <p>Supports both <em>strong</em> and <em>weak</em> validators as defined in
 * <a href="https://datatracker.ietf.org/doc/html/rfc7232#section-2.3">RFC 7232 §2.3</a>.</p>
 *
 * <ul>
 *   <li>Strong: {@code "etag123"}</li>
 *   <li>Weak:   {@code W/"etag123"}</li>
 * </ul>
 *
 * <p>ETags are primarily used in caching and conditional requests via
 * {@code If-None-Match} and {@code If-Match} headers.</p>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * // Create strong/weak tags programmatically:
 * ETag s = ETag.strong("abc123");      // renders as "abc123"
 * ETag w = ETag.weak("abc123");        // renders as W/"abc123"
 *
 * // Parse a single token:
 * ETag p1 = ETag.parse("W/\"abc\"");   // weak
 * ETag p2 = ETag.parse("\"abc\"");     // strong
 * ETag p3 = ETag.parse("abc");         // auto-quoted -> strong
 *
 * // Parse a comma-separated header:
 * List<ETag> list = ETag.parseList("W/\"x\", \"y\", z");
 *
 * // Compare:
 * s.strongEquals(ETag.strong("abc123")); // true
 * s.weakEquals(ETag.weak("abc123"));     // true (weak comparison ignores weakness)
 * }</pre>
 */
public final class ETag {

    /**
     * Quoted entity tag value (always stored in quoted form, e.g. {@code "\"abc123\""}).
     */
    private final String value;

    /**
     * Whether this tag is a weak validator (i.e., rendered as {@code W/<value>}).
     */
    private final boolean weak;

    private ETag(String value, boolean weak) {
        this.value = StringHelper.ensureQuoted(value);
        this.weak = weak;
    }

    /**
     * Parse a single ETag token from header context.
     *
     * <p>Accepts all of the following input forms:</p>
     * <ul>
     *   <li>{@code W/"abc"}</li>
     *   <li>{@code "abc"}</li>
     *   <li>{@code abc} (will be auto-quoted)</li>
     * </ul>
     *
     * <p>Returns {@code null} for invalid tokens (e.g. wildcard {@code *}).</p>
     *
     * @param token raw token (already trimmed)
     * @return parsed {@link ETag} or {@code null} if invalid
     */
    public static ETag parse(String token) {
        if (token == null || token.isEmpty()) return null;

        boolean weak = false;
        String etag = token;

        if (etag.startsWith("W/") || etag.startsWith("w/")) {
            weak = true;
            etag = etag.substring(2).trim();
        }

        // defend against bare wildcard or malformed value
        if ("*".equals(etag) || !isValidOpaque(etag)) {
            return null;
        }

        return ofCore(etag, weak);
    }

    /**
     * Parse a comma-separated {@code If-Match}/{@code If-None-Match} header value into a list of {@link ETag}s.
     *
     * <p>Invalid tokens (including {@code *}) are skipped. If you need to handle the special
     * wildcard semantics of {@code If-None-Match: *}, detect and treat it outside this method.</p>
     *
     * @param headerValue raw header string (may be {@code null}/blank)
     * @return list of parsed ETags (possibly empty)
     */
    public static List<ETag> parseAll(String headerValue) {
        List<ETag> eTags = new ArrayList<>();

        if (headerValue == null || headerValue.isBlank()) {
            return eTags;
        }

        String[] parts = headerValue.split(",");

        for (String token : parts) {
            if (token == null) {
                continue;
            }

            String trimmed = token.trim();

            if (trimmed.isEmpty() || "*".equals(trimmed)) {
                continue;
            }

            ETag tag = parse(trimmed);

            if (tag != null) {
                eTags.add(tag);
            }
        }

        return eTags;
    }

    /**
     * Minimal opaque-tag validation.
     *
     * <p>Rejects empty core and wildcard. You can extend this check to match RFC 7232 if needed
     * (e.g., validating DQUOTE rules or escaped characters).</p>
     */
    private static boolean isValidOpaque(String core) {
        return core != null && !core.isEmpty() && !"*".equals(core);
    }

    /**
     * Generic factory.
     *
     * @param value core value (quoted or unquoted)
     * @param weak  {@code true} for weak, {@code false} for strong
     * @return a new {@link ETag}
     */
    public static ETag ofCore(String value, boolean weak) {
        return new ETag(value, weak);
    }

    /**
     * Create a strong ETag.
     *
     * <p>The provided value may be quoted or unquoted. It will be stored in normalized
     * quoted form internally.</p>
     *
     * @param value the value (e.g., {@code "abc123"} or {@code "\"abc123\""})
     * @return a strong ETag instance
     */
    public static ETag strong(String value) {
        return ofCore(value, false);
    }

    /**
     * Create a weak ETag.
     *
     * <p>The provided value may be quoted or unquoted. It will be stored in normalized
     * quoted form internally.</p>
     *
     * @param value the value (e.g., {@code "abc123"} or {@code "\"abc123\""})
     * @return a weak ETag instance
     */
    public static ETag weak(String value) {
        return ofCore(value, true);
    }

    /**
     * @return {@code true} if this is a weak validator
     */
    public boolean isWeak() {
        return weak;
    }

    /**
     * @return quoted value string (always quoted, e.g. {@code "\"abc123\""})
     */
    public String value() {
        return value;
    }

    /**
     * Render this ETag in header format.
     *
     * @return a string suitable for the {@code ETag} header
     */
    public String toHeaderValue() {
        return weak ? "W/" + value : value;
    }

    /**
     * Strong comparison (octet-for-octet).
     *
     * <p>Both tags must be strong, and values must be identical.</p>
     *
     * @param that the ETag to compare with
     * @return {@code true} if strong equal, {@code false} otherwise
     */
    public boolean strongEquals(ETag that) {
        return that != null && !this.weak && !that.weak && this.value.equals(that.value);
    }

    /**
     * Weak comparison (semantic equivalence).
     *
     * <p>Only values are compared; weakness is ignored.</p>
     *
     * @param other the ETag to compare with
     * @return {@code true} if weakly equal, {@code false} otherwise
     */
    public boolean weakEquals(ETag other) {
        return other != null && this.value.equals(other.value);
    }

    @Override
    public String toString() {
        return toHeaderValue();
    }

    /**
     * Structural equality: both the (quoted) value and the weakness flag must match.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ETag etag)) {
            return false;
        }

        return strongEquals(etag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, weak);
    }
}
