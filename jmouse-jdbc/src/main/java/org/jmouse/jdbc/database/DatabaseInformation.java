package org.jmouse.jdbc.database;

/**
 * Immutable value object describing basic database product information.
 * <p>
 * {@code DatabaseInformation} typically represents metadata obtained from
 * {@link java.sql.DatabaseMetaData} and can be used for:
 * <ul>
 *     <li>dialect selection</li>
 *     <li>feature toggling</li>
 *     <li>logging and diagnostics</li>
 *     <li>version-based behavior branching</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * DatabaseMetaData meta = connection.getMetaData();
 *
 * DatabaseInformation info = new DatabaseInformation(
 *     meta.getDatabaseProductName(),
 *     meta.getDatabaseProductVersion(),
 *     meta.getDatabaseMajorVersion(),
 *     meta.getDatabaseMinorVersion()
 * );
 * }</pre>
 *
 * <p>
 * This record is intentionally minimal and transport-friendly.
 *
 * @param productName    database product name (e.g. {@code "PostgreSQL"})
 * @param productVersion full database version string
 * @param majorVersion   major version number
 * @param minorVersion   minor version number
 *
 * @author jMouse
 */
public record DatabaseInformation(
        String productName,
        String productVersion,
        int majorVersion,
        int minorVersion
) { }
