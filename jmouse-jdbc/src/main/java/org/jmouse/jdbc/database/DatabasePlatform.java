package org.jmouse.jdbc.database;

/**
 * Complete runtime database profile.
 *
 * <p>Encapsulates vendor identification, version, capabilities and SQL policies/builders.</p>
 */
public interface DatabasePlatform {

    DatabaseId id();

    DatabaseVersion version();

    DatabaseCapabilities capabilities();

    SqlQuoting quoting();

    SqlTemplates sql();

    /**
     * Optional hook point for vendor-specific SQL normalization/rewrite.
     */
    default RewriteHook rewriteHook() {
        return RewriteHook.noop();
    }

    /**
     * Human-readable label for logs/diagnostics.
     */
    default String displayName() {
        return id().vendor() + ":" + id().product() + " " + version();
    }
}
