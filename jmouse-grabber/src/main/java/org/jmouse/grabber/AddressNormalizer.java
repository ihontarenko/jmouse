package org.jmouse.grabber;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;

import org.jmouse.core.matcher.Matcher;
import org.jmouse.core.matcher.TextMatchers;

/**
 * 🧭 Turns two spellings of the same page into one address.
 *
 * <p>A grabber that does not do this fetches {@code /product/7}, {@code /product/7#reviews} and
 * {@code /product/7?utm_source=mail} as three pages, and the third of those is the one that fills a
 * journal with rubbish. So normalisation is on by default and is configurable rather than optional.</p>
 *
 * <p>What the default does:</p>
 * <ul>
 *   <li>lower-cases the scheme and the host, which are case-insensitive by specification;</li>
 *   <li>drops the port when it is the scheme's own default;</li>
 *   <li>drops the fragment, which the server never sees;</li>
 *   <li>drops a trailing slash on a non-empty path, so {@code /catalog} and {@code /catalog/} are one;</li>
 *   <li>sorts the query parameters and drops the ones a matcher rejects — tracking parameters by
 *       default.</li>
 * </ul>
 *
 * <p>⚠️ <b>Sorting the query is a real decision, not tidying.</b> It makes {@code ?a=1&amp;b=2} and
 * {@code ?b=2&amp;a=1} one key, which is right for nearly every site and wrong for the rare one whose
 * parameter order is meaningful. {@link #keepingQueryOrder()} is there for that site.</p>
 */
public final class AddressNormalizer {

    /**
     * 🚮 The parameters dropped by default: the tracking ones that change per visitor and never
     * change the page.
     */
    public static final List<String> TRACKING_PARAMETERS = List.of(
            "utm_*", "gclid", "fbclid", "msclkid", "yclid", "_ga", "mc_cid", "mc_eid", "ref", "referrer"
    );

    private static final Map<String, Integer> DEFAULT_PORTS = Map.of("http", 80, "https", 443);

    private final Matcher<String> droppedParameter;
    private final boolean         sortQuery;
    private final boolean         dropFragment;
    private final boolean         dropTrailingSlash;

    private AddressNormalizer(
            Matcher<String> droppedParameter, boolean sortQuery, boolean dropFragment, boolean dropTrailingSlash) {
        this.droppedParameter = droppedParameter;
        this.sortQuery = sortQuery;
        this.dropFragment = dropFragment;
        this.dropTrailingSlash = dropTrailingSlash;
    }

    /**
     * 🧭 The default: everything described above, dropping {@link #TRACKING_PARAMETERS}.
     */
    public static AddressNormalizer standard() {
        return new AddressNormalizer(trackingParameterMatcher(), true, true, true);
    }

    /**
     * 🪞 Changes nothing at all — for a site whose addresses are already canonical, or one where
     * every character matters.
     */
    public static AddressNormalizer identity() {
        return new AddressNormalizer(Matcher.constant(false), false, false, false);
    }

    /**
     * 🚮 The default, dropping these parameter names as well. Each may be an ant pattern.
     */
    public AddressNormalizer dropping(String... parameterNames) {
        Matcher<String> extra = anyOf(Arrays.asList(parameterNames));
        return new AddressNormalizer(droppedParameter.or(extra), sortQuery, dropFragment, dropTrailingSlash);
    }

    /**
     * 🚮 Drops exactly the parameters this matcher accepts, replacing the tracking list.
     */
    public AddressNormalizer droppingParameters(Matcher<String> matcher) {
        return new AddressNormalizer(matcher, sortQuery, dropFragment, dropTrailingSlash);
    }

    /**
     * 🔢 Leaves the query parameters in the order they were written.
     */
    public AddressNormalizer keepingQueryOrder() {
        return new AddressNormalizer(droppedParameter, false, dropFragment, dropTrailingSlash);
    }

    /**
     * 🧭 The normalised form of this address.
     *
     * <p>An address that cannot be re-assembled comes back unchanged rather than throwing: a
     * grabber that dies on one odd link is worse than one that visits it verbatim.</p>
     */
    public URI normalize(URI address) {
        try {
            String scheme = lowerCase(address.getScheme());
            String host   = lowerCase(address.getHost());
            int    port   = normalizePort(scheme, address.getPort());
            String path   = normalizePath(address.getPath());
            String query  = normalizeQuery(address.getQuery());
            String target = dropFragment ? null : address.getFragment();

            if (host == null) {
                return address;
            }

            return new URI(scheme, address.getUserInfo(), host, port, path, query, target);
        } catch (URISyntaxException exception) {
            return address;
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        if (dropTrailingSlash && path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }

        return path;
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        Map<String, String> sorted = new TreeMap<>();
        Set<String>         kept   = new LinkedHashSet<>();

        for (String parameter : query.split("&")) {
            if (parameter.isBlank()) {
                continue;
            }

            int    separator = parameter.indexOf('=');
            String name      = separator < 0 ? parameter : parameter.substring(0, separator);

            if (droppedParameter.matches(name)) {
                continue;
            }

            if (sortQuery) {
                sorted.put(name, parameter);
            } else {
                kept.add(parameter);
            }
        }

        Collection<String> parameters = sortQuery ? sorted.values() : kept;

        if (parameters.isEmpty()) {
            return null;
        }

        return String.join("&", parameters);
    }

    private int normalizePort(String scheme, int port) {
        if (port < 0) {
            return -1;
        }

        Integer standard = scheme == null ? null : DEFAULT_PORTS.get(scheme);

        if (standard != null && standard == port) {
            return -1;
        }

        return port;
    }

    private String lowerCase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static Matcher<String> trackingParameterMatcher() {
        return anyOf(TRACKING_PARAMETERS);
    }

    private static Matcher<String> anyOf(Collection<String> patterns) {
        Matcher<String> matcher = Matcher.constant(false);

        for (String pattern : patterns) {
            if (pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0) {
                matcher = matcher.or(TextMatchers.ant(pattern));
            } else {
                matcher = matcher.or(TextMatchers.same(pattern));
            }
        }

        return matcher;
    }

}
