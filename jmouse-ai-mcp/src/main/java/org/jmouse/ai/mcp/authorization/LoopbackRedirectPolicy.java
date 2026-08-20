package org.jmouse.ai.mcp.authorization;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Where an approved client may be sent back to, and nowhere else.
 *
 * <p>The flow hands a <strong>live authorization code</strong> to whatever address the caller named, so
 * this is the one thing standing between it and an open redirect that leaks that code. Which is why the
 * allow-list is exact-match — a listed loopback host, an explicit high port, one of a few exact paths,
 * no query and no fragment — rather than a prefix or a pattern. ⚠️ A permissive rule here would not be
 * a smaller version of this guard; it would be no guard at all.
 *
 * <p>The one rule an installation may widen is the path, and only to what a client hangs
 * <em>underneath</em> a listed one — see {@link #allowNestedPaths}. It is off unless somebody turns it
 * on, because a guard that ships loose is a guard nobody chose.
 *
 * <p>Every refusal names what would have been accepted. The caller is a program: it can correct itself
 * and retry, but only if it is told what it got wrong.
 *
 * @param allowedHosts     the loopback names a client may be returned to, e.g. {@code 127.0.0.1}
 * @param allowedPaths     the exact paths, e.g. {@code /callback}
 * @param allowNestedPaths whether a client may also be sent to its own segments under a listed path —
 *                         {@code /callback/7f3a}, as Codex names, with a fresh last segment every run and
 *                         so no spelling of it to list. Nothing is given away by allowing it: those
 *                         segments are routes inside the client's <em>own</em> loopback listener, and the
 *                         four rules that carry the weight stay exactly as strict. The extra segments are
 *                         held to unreserved characters, which keeps percent-encoding and {@code ..} out
 *                         of an address a person is about to read on an approval screen.
 */
public record LoopbackRedirectPolicy(
        List<String> allowedHosts,
        List<String> allowedPaths,
        boolean      allowNestedPaths
) {

    private static final String  HTTP_SCHEME   = "http";
    private static final int     LOWEST_PORT   = 1024;
    private static final int     HIGHEST_PORT  = 65535;
    private static final Pattern PLAIN_SEGMENT = Pattern.compile("[A-Za-z0-9._~-]+");
    private static final Set<String> DOT_SEGMENTS = Set.of(".", "..");

    public LoopbackRedirectPolicy {
        allowedHosts = List.copyOf(allowedHosts);
        allowedPaths = List.copyOf(allowedPaths);
    }

    /** What a client on this machine conventionally listens on. */
    public static LoopbackRedirectPolicy loopbackOnly(List<String> allowedPaths, boolean allowNestedPaths) {
        return new LoopbackRedirectPolicy(
                List.of("127.0.0.1", "localhost", "[::1]"), allowedPaths, allowNestedPaths);
    }

    /**
     * Parses and vets a redirect target, returning it only if every rule holds.
     *
     * @throws McpAuthorizationException naming the rule that failed and the whole shape that would pass
     */
    public URI require(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw refusalOf("nothing", "no redirect address was given");
        }

        URI target = parse(candidate);

        if (!HTTP_SCHEME.equalsIgnoreCase(target.getScheme())) {
            throw refusalOf(candidate, "the scheme must be http");
        }

        if (target.getUserInfo() != null) {
            throw refusalOf(candidate, "credentials in the address are not allowed");
        }

        if (allowedHosts.stream().noneMatch(host -> host.equalsIgnoreCase(target.getHost()))) {
            throw refusalOf(candidate, "the host must be one of " + String.join(", ", allowedHosts));
        }

        // Explicitly stated, because a missing port is -1 and a default port is somebody else's server.
        if (target.getPort() < LOWEST_PORT || target.getPort() > HIGHEST_PORT) {
            throw refusalOf(candidate, "the port must be stated explicitly and be between "
                                     + LOWEST_PORT + " and " + HIGHEST_PORT);
        }

        // ⚠️ The raw path, not the decoded one: a decoded '?' or '#' would be judged here as an ordinary
        // character and then travel as a real delimiter in the callback URL.
        if (!isAcceptablePath(target.getRawPath())) {
            throw refusalOf(candidate, pathRule());
        }

        if (target.getRawQuery() != null || target.getRawFragment() != null) {
            throw refusalOf(candidate, "the address must carry no query string and no fragment");
        }

        return target;
    }

    /**
     * The URL the browser is finally sent to.
     *
     * <p>Appended rather than merged, and that is safe only because {@link #require} has already
     * established the target carries no query of its own — which is one of the reasons it insists.
     */
    public String callbackUrl(URI target, String code, String state) {
        StringBuilder url = new StringBuilder(target.toString())
                .append("?code=").append(encode(code));

        if (state != null && !state.isBlank()) {
            url.append("&state=").append(encode(state));
        }

        return url.toString();
    }

    /** How the target reads on an approval screen: the address the code will go to. */
    public String describe(URI target) {
        return target.getHost() + ":" + target.getPort() + target.getPath();
    }

    /** A listed path — or, where an installation allows it, a client's own segments under a listed one. */
    private boolean isAcceptablePath(String path) {
        if (path == null) {
            return false;
        }

        if (allowedPaths.contains(path)) {
            return true;
        }

        return allowNestedPaths
            && allowedPaths.stream().anyMatch(allowed -> isNestedUnder(path, allowed));
    }

    private static boolean isNestedUnder(String path, String allowed) {
        String separator = allowed.endsWith("/") ? allowed : allowed + "/";

        if (!path.startsWith(separator)) {
            return false;
        }

        return arePlainSegments(path.substring(separator.length()));
    }

    /**
     * Unreserved characters and nothing else — no percent-encoding, no empty segment, no dot segment.
     *
     * <p>Which is what makes the nested path harmless twice over: it cannot smuggle a delimiter past the
     * checks above, and it cannot climb out of the path it was allowed under.
     */
    private static boolean arePlainSegments(String remainder) {
        for (String segment : remainder.split("/", -1)) {
            if (DOT_SEGMENTS.contains(segment) || !PLAIN_SEGMENT.matcher(segment).matches()) {
                return false;
            }
        }

        return true;
    }

    /** What a caller has to spell instead, stated the way this installation actually judges it. */
    private String pathRule() {
        String listed = String.join(" or ", allowedPaths);

        return allowNestedPaths
                ? "the path must be " + listed + ", optionally followed by the client's own segments"
                : "the path must be exactly " + listed;
    }

    private URI parse(String candidate) {
        try {
            URI target = new URI(candidate);

            if (!target.isAbsolute()) {
                throw refusalOf(candidate, "the address must be absolute");
            }

            return target;

        } catch (URISyntaxException unreadable) {
            throw refusalOf(candidate, "the address could not be read as a URL");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private McpAuthorizationException refusalOf(String candidate, String reason) {
        return new McpAuthorizationException(
                "That redirect address is refused: " + reason + ". A client may only be sent back to a "
                + "loopback address on this machine — http://" + allowedHosts.getFirst() + ":<port>"
                + allowedPaths.getFirst() + (allowNestedPaths ? "[/...]" : "")
                + " — with no query string and no fragment. Received: " + candidate);
    }
}
