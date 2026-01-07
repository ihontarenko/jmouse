package org.jmouse.jdbc.database;

/**
 * Normalized representation of a database version.
 * <p>
 * {@code DatabaseVersion} encapsulates both structured numeric components
 * (major / minor / patch) and the original raw version string as reported
 * by the JDBC driver.
 *
 * <p>
 * This abstraction is useful for:
 * <ul>
 *     <li>version-based feature toggling</li>
 *     <li>dialect or SQL capability decisions</li>
 *     <li>human-readable diagnostics and logging</li>
 * </ul>
 *
 * <h3>Unknown versions</h3>
 * <p>
 * When a version cannot be reliably parsed, {@link #unknown(String)} may be used.
 * In this case, all numeric components are set to {@code 0} and {@link #toString()}
 * falls back to the raw value.
 *
 * <h3>String representation</h3>
 * <ul>
 *     <li>{@code major.minor.patch} if patch &gt; 0</li>
 *     <li>{@code major.minor} if patch == 0</li>
 *     <li>{@code raw} or {@code "unknown"} if numeric components are all zero</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * DatabaseVersion v1 = new DatabaseVersion(15, 3, 0, "15.3");
 * DatabaseVersion v2 = DatabaseVersion.unknown("PostgreSQL 15beta1");
 *
 * v1.toString(); // "15.3"
 * v2.toString(); // "PostgreSQL 15beta1"
 * }</pre>
 *
 * @param major major version number
 * @param minor minor version number
 * @param patch patch version number (0 if not applicable)
 * @param raw   raw version string as reported by the database/driver
 *
 * @author jMouse
 */
public record DatabaseVersion(int major, int minor, int patch, String raw) {

    /**
     * Creates an {@code unknown} database version.
     * <p>
     * Numeric components are set to {@code 0} and the provided raw
     * version string is preserved for diagnostics.
     *
     * @param raw raw version string
     * @return unknown database version
     */
    public static DatabaseVersion unknown(String raw) {
        return new DatabaseVersion(0, 0, 0, raw);
    }

    /**
     * Returns a human-readable version string.
     * <p>
     * Formatting rules:
     * <ul>
     *     <li>If all numeric components are zero, returns {@code raw} or {@code "unknown"}</li>
     *     <li>Otherwise returns {@code major.minor} or {@code major.minor.patch}</li>
     * </ul>
     *
     * @return formatted version string
     */
    @Override
    public String toString() {
        if (major == 0 && minor == 0 && patch == 0) {
            return raw != null ? raw : "unknown";
        }
        return "%d.%d%s".formatted(major, minor, (patch > 0 ? (".%d".formatted(patch)) : ""));
    }

}
