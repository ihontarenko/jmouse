package org.jmouse.jdbc.platform;

import java.util.Optional;

/**
 * Inputs required for dialect resolution.
 */
public record DialectInputs(

        /**
         * Dialect id from configuration (if any).
         */
        Optional<String> configuredDialectId,

        /**
         * Whether metadata-based resolution is allowed.
         */
        boolean allowMetadata,

        /**
         * Whether platform can provide metadata (some environments may restrict it).
         */
        boolean metadataAvailable
) {
    public static DialectInputs configured(String dialectId, boolean allowMetadata) {
        return new DialectInputs(Optional.ofNullable(dialectId), allowMetadata, true);
    }

    public static DialectInputs metadataOnly() {
        return new DialectInputs(Optional.empty(), true, true);
    }

    public static DialectInputs fixedOnly(String dialectId) {
        return new DialectInputs(Optional.ofNullable(dialectId), false, false);
    }
}
