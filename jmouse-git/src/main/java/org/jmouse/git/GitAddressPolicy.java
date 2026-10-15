package org.jmouse.git;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Which remote addresses may be reached at all — an installation decision, fixed when a {@link GitClient} is made.
 *
 * <p>Whatever the schemes admitted, an address is always refused when it carries user information, a query or a
 * fragment, and a token is only ever sent over https. With {@code publicHostsOnly}, every address the host name
 * resolves to must be public: loopback, private, link-local, carrier-grade and reserved ranges are refused, and so
 * is {@code file://}. The check resolves the name itself and JGit resolves it again when it connects, so a name
 * whose answers change in between (DNS rebinding) is not fully excluded; a deployment that needs that guarantee
 * puts the worker behind an egress proxy.
 *
 * @param schemes         the schemes admitted; never empty
 * @param publicHostsOnly whether hosts on a private network are refused
 */
public record GitAddressPolicy(Set<GitScheme> schemes, boolean publicHostsOnly) {

    /** https to any host: the default. */
    public static final GitAddressPolicy HTTPS = new GitAddressPolicy(Set.of(GitScheme.HTTPS), false);

    /** https to public hosts only: for addresses somebody typed that a server will fetch. */
    public static final GitAddressPolicy PUBLIC_HTTPS = new GitAddressPolicy(Set.of(GitScheme.HTTPS), true);

    public GitAddressPolicy {
        // Immutable sets throw on contains(null), so the null check walks the elements instead.
        if (schemes == null || schemes.isEmpty() || schemes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("An address policy admits at least one scheme.");
        }
        schemes = Set.copyOf(EnumSet.copyOf(schemes));
    }

    /** A policy admitting these schemes to any host — {@code permitting(GitScheme.FILE)} is what a test uses. */
    public static GitAddressPolicy permitting(GitScheme... schemes) {
        return new GitAddressPolicy(Set.of(schemes), false);
    }

    /**
     * The remote's address, once it is known to be allowed.
     *
     * @throws GitException {@link GitException.Reason#ADDRESS_NOT_ALLOWED} when it is not, or
     *                      {@link GitException.Reason#NOT_FOUND} when a public-only policy cannot resolve its host
     */
    public URI requireAllowed(GitRemote remote) {
        URI address = remote.address();
        GitScheme scheme = GitScheme.of(address.getScheme()).orElseThrow(GitAddressPolicy::notAllowed);
        if (!schemes.contains(scheme) || !address.isAbsolute() || address.isOpaque()) {
            throw notAllowed();
        }
        if (address.getRawUserInfo() != null || address.getRawQuery() != null || address.getRawFragment() != null) {
            throw notAllowed();
        }
        if (remote.credentials() instanceof GitCredentials.AccessToken && scheme != GitScheme.HTTPS) {
            throw notAllowed();
        }
        String path = address.getPath();
        if (path == null || path.length() < 2) {
            throw notAllowed();
        }
        if (scheme == GitScheme.FILE) {
            if (address.getHost() != null || publicHostsOnly) {
                throw notAllowed();
            }
            return address;
        }
        if (address.getHost() == null || address.getHost().isBlank()) {
            throw notAllowed();
        }
        if (publicHostsOnly) {
            requirePublicHost(address.getHost());
        }
        return address;
    }

    private static void requirePublicHost(String host) {
        InetAddress[] resolved;
        try {
            resolved = InetAddress.getAllByName(host);
        } catch (UnknownHostException | SecurityException failure) {
            throw new GitException(GitException.Reason.NOT_FOUND);
        }
        for (InetAddress candidate : resolved) {
            if (!isPublic(candidate)) {
                throw notAllowed();
            }
        }
    }

    /** Whether an address is reachable on the public internet rather than on this machine's own networks. */
    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first  = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            int third  = bytes[2] & 0xff;
            boolean thisNetwork         = first == 0;
            boolean sharedCarrierSpace  = first == 100 && second >= 64 && second <= 127;
            boolean protocolAssignments = first == 192 && second == 0 && third == 0;
            boolean benchmarking        = first == 198 && (second == 18 || second == 19);
            boolean reserved            = first >= 240;
            return !(thisNetwork || sharedCarrierSpace || protocolAssignments || benchmarking || reserved);
        }
        if (address instanceof Inet6Address) {
            // fc00::/7 unique local addresses are private the way 10/8 is.
            return (bytes[0] & 0xfe) != 0xfc;
        }
        return true;
    }

    private static GitException notAllowed() {
        return new GitException(GitException.Reason.ADDRESS_NOT_ALLOWED);
    }
}
