package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link KeyExtractor} implementation for extracting a single {@link Long}
 * generated key.
 * <p>
 * {@code SingleLongKeyExtractor} reads the first column of the first row
 * from the generated-keys {@link ResultSet} and returns it as a {@link Long}.
 *
 * <p>
 * Extraction semantics:
 * <ul>
 *     <li>no rows → returns {@code null}</li>
 *     <li>at least one row → returns {@code keys.getLong(1)}</li>
 * </ul>
 *
 * <p>
 * This extractor is intended for the common case of auto-increment numeric
 * primary keys.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * Long id = jdbc.update(
 *     "insert into users(name) values(?)",
 *     ps -> ps.setString(1, "Alice"),
 *     new SingleLongKeyExtractor()
 * );
 * }</pre>
 *
 * <p>
 * Note: SQL {@code NULL} handling is delegated to the JDBC driver.
 * If the first generated key is {@code NULL}, {@code 0} may be returned
 * depending on driver behavior; callers may wish to validate accordingly.
 *
 * @author jMouse
 */
public final class SingleLongKeyExtractor implements KeyExtractor<Long> {

    /**
     * Extracts a single {@link Long} key from the generated-keys {@link ResultSet}.
     *
     * @param keys result set containing generated keys
     * @return generated key value, or {@code null} if no keys are present
     * @throws SQLException if JDBC access fails during extraction
     */
    @Override
    public Long extract(ResultSet keys) throws SQLException {
        return keys.next() ? keys.getLong(1) : null;
    }

}
