package org.jmouse.jdbc.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Strategy interface for extracting generated keys from a JDBC operation.
 * <p>
 * {@code KeyExtractor} is used for INSERT (or similar) statements that
 * return auto-generated keys via {@link java.sql.Statement#getGeneratedKeys()}.
 *
 * <p>
 * The extractor is invoked with a {@link ResultSet} containing the generated
 * keys. Implementations are free to extract:
 * <ul>
 *     <li>a single key</li>
 *     <li>multiple keys</li>
 *     <li>a composite key representation</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * KeyExtractor<Long> idExtractor = keys ->
 *     keys.next() ? keys.getLong(1) : null;
 *
 * Long id = jdbc.update(
 *     "insert into users(name) values(?)",
 *     ps -> ps.setString(1, "Alice"),
 *     idExtractor
 * );
 * }</pre>
 *
 * <p>
 * Note: The {@link ResultSet} lifecycle is managed by the executor/template.
 * Implementations should not close the result set.
 *
 * @param <K> extracted key type
 *
 * @author jMouse
 */
@FunctionalInterface
public interface KeyExtractor<K> {

    /**
     * Extracts a key (or keys) from the generated-keys {@link ResultSet}.
     * <p>
     * Implementations are responsible for advancing the cursor
     * (e.g. calling {@link ResultSet#next()}).
     *
     * @param keys result set containing generated keys
     * @return extracted key value (or {@code null} if none)
     * @throws SQLException if JDBC access fails during extraction
     */
    K extract(ResultSet keys) throws SQLException;

}
