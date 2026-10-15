package org.jmouse.git;

import java.util.Locale;
import java.util.Optional;

/** The address schemes a {@link GitAddressPolicy} can admit. ssh is not supported yet. */
public enum GitScheme {

    /** The default and the only scheme over which a token is ever sent. */
    HTTPS,

    /** Plain http: readable on the wire, so admitted only for anonymous reads and only when a policy opts in. */
    HTTP,

    /** A repository on the local file system, {@code file://}; meant for tests and local mirrors. */
    FILE;

    /** The scheme an address names, if it is one of these. */
    public static Optional<GitScheme> of(String scheme) {
        if (scheme == null) {
            return Optional.empty();
        }
        return switch (scheme.toLowerCase(Locale.ROOT)) {
            case "https" -> Optional.of(HTTPS);
            case "http" -> Optional.of(HTTP);
            case "file" -> Optional.of(FILE);
            default -> Optional.empty();
        };
    }
}
