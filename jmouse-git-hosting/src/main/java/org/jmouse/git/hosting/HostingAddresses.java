package org.jmouse.git.hosting;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Address rules both hostings share: what a base address may be, and how an address is cut into path segments. */
final class HostingAddresses {

    private HostingAddresses() {
    }

    /** An https base address with a host and nothing after its path, without a trailing slash. */
    static URI requireBase(URI base) {
        if (base == null || !"https".equalsIgnoreCase(base.getScheme()) || base.getHost() == null
                || base.getRawUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null) {
            throw new HostingException(HostingException.Reason.CONFIGURATION);
        }
        String path = base.getRawPath() == null ? "" : base.getRawPath();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return URI.create("https://" + base.getRawAuthority().toLowerCase(Locale.ROOT) + path);
    }

    /** The host as a {@link RepositoryReference} records it: lowercase, with a port only when one is given. */
    static String hostOf(URI base) {
        return base.getRawAuthority().toLowerCase(Locale.ROOT);
    }

    /**
     * The path segments of an address under a base, or a refusal when the address is not an https address of that
     * base. Empty segments (a trailing or doubled slash) are dropped.
     */
    static List<String> segmentsUnder(URI base, String address) {
        URI parsed;
        try {
            parsed = URI.create(address == null ? "" : address.strip());
        } catch (IllegalArgumentException failure) {
            throw notRecognised();
        }
        if (!"https".equalsIgnoreCase(parsed.getScheme()) || parsed.getRawAuthority() == null
                || parsed.getRawUserInfo() != null || !hostOf(base).equals(hostOf(parsed))) {
            throw notRecognised();
        }
        String basePath = base.getPath() == null ? "" : base.getPath();
        String path     = parsed.getPath() == null ? "" : parsed.getPath();
        if (!path.startsWith(basePath + "/")) {
            throw notRecognised();
        }
        List<String> segments = new ArrayList<>();
        for (String segment : path.substring(basePath.length()).split("/")) {
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        return segments;
    }

    /** A repository name without the {@code .git} a clone address ends with. */
    static String withoutGitSuffix(String name) {
        if (name.length() > 4 && name.toLowerCase(Locale.ROOT).endsWith(".git")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    static HostingException notRecognised() {
        return new HostingException(HostingException.Reason.ADDRESS_NOT_RECOGNISED);
    }
}
