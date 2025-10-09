package org.jmouse.web.http;

import org.jmouse.util.StringHelper;

import java.util.*;

/**
 * 📑 Representation of the HTTP {@code Vary} response header.
 *
 * <p>The {@code Vary} header indicates which request headers a cache
 * should take into account when deciding whether a stored response
 * can be reused for subsequent requests.</p>
 *
 * <ul>
 *   <li>{@code Vary: *} → disables shared caching (wildcard)</li>
 *   <li>{@code Vary: Accept-Encoding, Accept-Language} → varies by listed headers</li>
 *   <li>Empty instance → no {@code Vary} header emitted</li>
 * </ul>
 *
 * <p>Instances are immutable and provide idempotent merge/with operations.</p>
 *
 * @see <a href="https://developer.mozilla.org/docs/Web/HTTP/Headers/Vary">MDN: Vary header</a>
 */
public final class Vary {

    public static final String ASTRIX = "*";

    private final List<HttpHeader> headers;

    /**
     * 📌 True if this represents {@code Vary: *}.
     */
    private final boolean wildcard;

    public Vary(List<HttpHeader> headers, boolean wildcard) {
        this.headers = Collections.unmodifiableList(headers);
        this.wildcard = wildcard;
    }

    /**
     * ➖ Empty Vary (no header will be emitted).
     *
     * @return empty vary instance
     */
    public static Vary empty() {
        return new Vary(List.of(), false);
    }

    /**
     * ✳️ Wildcard Vary → {@code Vary: *}.
     *
     * @return wildcard vary instance
     */
    public static Vary any() {
        return new Vary(List.of(), true);
    }

    /**
     * Create from a Collection of header names. Deduplicates while preserving order.
     *
     * @param headers header names
     * @return vary instance
     */
    public static Vary of(Collection<HttpHeader> headers) {
        if (headers == null || headers.isEmpty()) {
            return empty();
        }
        Set<HttpHeader> unique = new LinkedHashSet<>(headers);
        return new Vary(List.copyOf(unique), false);
    }

    /**
     * Create from varargs of header names.
     */
    public static Vary of(HttpHeader... headers) {
        return of(List.of(headers));
    }

    /**
     * Parse from a raw header string (e.g. {@code "*"} or {@code "Accept-Encoding, Accept-Language"}).
     *
     * @param header raw header value
     * @return vary instance
     */
    public static Vary of(String header) {
        String raw = header.trim();

        if (raw.startsWith(ASTRIX)) {
            return any();
        }

        String[]         tokens  = StringHelper.tokenize(header, ",");
        List<HttpHeader> headers = new ArrayList<>();

        for (String name : tokens) {
            if (ASTRIX.equals(name)) {
                return any();
            }
            headers.add(HttpHeader.ofHeader(name));
        }

        return of(headers);
    }

    /**
     * @return {@code true} if this equals {@code Vary: *}
     */
    public boolean isWildcard() {
        return wildcard;
    }

    /**
     * @return {@code true} if no headers are present and not wildcard
     */
    public boolean isEmpty() {
        return !wildcard && headers.isEmpty();
    }

    /**
     * @return immutable list of header names
     */
    public List<HttpHeader> asList() {
        return headers;
    }

    /**
     * Check if this set headers the given header.
     *
     * @param header header to test
     * @return {@code true} if contained
     */
    public boolean contains(HttpHeader header) {
        return headers.contains(header);
    }

    /**
     * 🔗 Merge with another {@code Vary}.
     *
     * <p>Union of header names; wildcard dominates.</p>
     *
     * @param that other vary
     * @return new combined vary
     */
    public Vary merge(Vary that) {
        if (this.isWildcard() || that.wildcard) {
            return any();
        }

        if (that.headers.isEmpty()) {
            return this;
        }

        if (this.headers.isEmpty()) {
            return that;
        }

        List<HttpHeader> headers = new ArrayList<>(this.headers);
        headers.addAll(that.headers);

        return of(headers);
    }

    /**
     * ➗ Intersection of two Allow sets.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>ANY ∩ ANY = ANY</li>
     *   <li>ANY ∩ X = X</li>
     *   <li>X ∩ ANY = X</li>
     *   <li>X ∩ Y = X ∩ Y (set intersection)</li>
     * </ul>
     *
     * @param that the other Allow
     * @return intersection result
     */
    public Vary intersect(Vary that) {
        if (this.isWildcard() && that.isWildcard()) {
            return any();
        }

        if (this.isWildcard()) {
            return of(that.headers);
        }

        if (that.isWildcard()) {
            return of(this.headers);
        }

        Set<HttpHeader> intersect = new HashSet<>();

        for (HttpHeader httpHeader : headers) {
            if (that.contains(httpHeader)) {
                intersect.add(httpHeader);
            }
        }

        return of(List.copyOf(intersect));
    }

    /**
     * ➖ Set difference (this \ that).
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>ANY \ ANY = ∅</li>
     *   <li>X \ ANY = ∅</li>
     *   <li>ANY \ X = ALL \ X (use the HttpMethod universe)</li>
     *   <li>X \ Y = standard set difference</li>
     * </ul>
     *
     * @param that the other Allow
     * @return difference result
     */
    public Vary difference(Vary that) {
        if (this.isWildcard() && that.isWildcard()) {
            return empty(); // X \ ANY = ∅
        }

        if (this.isWildcard()) {
            Set<HttpHeader> universe = EnumSet.allOf(HttpHeader.class);
            that.headers.forEach(universe::remove);
            return of(List.copyOf(universe));
        }

        Set<HttpHeader> difference = new HashSet<>(this.headers);

        for (HttpHeader header : that.headers) {
            difference.remove(header);
        }

        return of(List.copyOf(difference));
    }

    /**
     * ➕ Return a new {@code Vary} with additional header names.
     *
     * @param more headers to add
     * @return new vary instance
     */
    public Vary with(HttpHeader... more) {
        if (this.wildcard) {
            return this;
        }

        if (more == null || more.length == 0) {
            return this;
        }

        List<HttpHeader> headers = new ArrayList<>(this.headers);
        Collections.addAll(headers, more);
        return of(headers);
    }

    /**
     * Render to a single header-line value.
     *
     * <ul>
     *   <li>{@code *}</li>
     *   <li>{@code Accept-Encoding, Accept-Language}</li>
     *   <li>{@code ""} if empty</li>
     * </ul>
     *
     * @return header value string
     */
    public String toHeaderValue() {
        if (wildcard) {
            return ASTRIX;
        }

        if (headers.isEmpty()) {
            return "";
        }

        StringJoiner joiner = new StringJoiner(", ");
        headers.stream().map(HttpHeader::toString).forEach(joiner::add);
        return joiner.toString();
    }

    /**
     * Write this vary value into the given buffered headers.
     *
     * @param headers buffer to write into
     */
    public void writeTo(Headers headers) {
        if (!isEmpty()) {
            headers.setHeader(HttpHeader.VARY, toHeaderValue());
        }
    }

    @Override
    public String toString() {
        return "Vary[%s]".formatted(wildcard ? "*" : toHeaderValue());
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }

        if (!(that instanceof Vary vary)) {
            return false;
        }

        return wildcard == vary.wildcard && headers.equals(vary.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wildcard, headers);
    }
}
