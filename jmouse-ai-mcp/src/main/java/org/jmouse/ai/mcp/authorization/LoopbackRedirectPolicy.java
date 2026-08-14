package org.jmouse.ai.mcp.authorization;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Where an approved client may be sent back to, and nowhere else.
 *
 * <p>The flow hands a <strong>live authorization code</strong> to whatever address the caller named, so
 * this is the one thing standing between it and an open redirect that leaks that code. Which is why the
 * allow-list is exact-match — a listed loopback host, an explicit high port, one of a few exact paths,
 * no query and no fragment — rather than a prefix or a pattern. ⚠️ A permissive rule here would not be
 * a smaller version of this guard; it would be no guard at all.
 *
 * <p>Every refusal names what would have been accepted. The caller is a program: it can correct itself
 * and retry, but only if it is told what it got wrong.
 *
 * @param allowedHosts the loopback names a client may be returned to, e.g. {@code 127.0.0.1}
 * @param allowedPaths the exact paths, e.g. {@code /callback}
 */
public record LoopbackRedirectPolicy(List<String> allowedHosts, List<String> allowedPaths) {

    private static final String HTTP_SCHEME  = "http";
    private static final int    LOWEST_PORT  = 1024;
    private static final int    HIGHEST_PORT = 65535;

    public LoopbackRedirectPolicy {
        allowedHosts = List.copyOf(allowedHosts);
        allowedPaths = List.copyOf(allowedPaths);
    }

    /** What a client on this machine conventionally listens on. */
    public static LoopbackRedirectPolicy loopbackOnly(List<String> allowedPaths) {
        return new LoopbackRedirectPolicy(List.of("127.0.0.1", "localhost", "[::1]"), allowedPaths);
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

        if (!allowedPaths.contains(target.getPath())) {
            throw refusalOf(candidate, "the path must be exactly " + String.join(" or ", allowedPaths));
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
                + allowedPaths.getFirst() + " — with no query string and no fragment. Received: "
                + candidate);
    }
}
