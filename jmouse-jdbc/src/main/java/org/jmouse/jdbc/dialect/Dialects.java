package org.jmouse.jdbc.dialect;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Which database this is — asked, never guessed.
 *
 * <h2>⚠️ Why this is worth a class</h2>
 *
 * <p>Getting it wrong is not a syntax error somebody notices. The two dialects differ in how an interval
 * is written and in how text is read as a number, so a statement built for the wrong one either fails
 * far from here or — worse — runs and answers about a different length of time. So the question is
 * asked of the connection, and a database nobody has a dialect for is refused by name rather than
 * silently defaulted to whichever was written first.</p>
 *
 * <p>⚠️ It lives here rather than in each caller because two already needed it: the Spring bridge, which
 * has a {@code jmouse.query.dialect} property and falls back to asking, and a product that builds its
 * own engines and has no property at all. Two copies of "does the product name contain 'maria'?" is two
 * places for the answer to differ.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class Dialects {

    private Dialects() {
    }

    /**
     * The dialect for a database product name — {@code MySQL}, {@code PostgreSQL}, and what a driver's
     * metadata actually reports.
     *
     * <p>⚠️ Matched loosely on purpose: drivers report {@code "MySQL"}, {@code "MariaDB"} and
     * {@code "PostgreSQL"} but also longer strings, and a product that spelled it exactly would break on
     * a driver upgrade for no reason anybody could have predicted.</p>
     *
     * @param product what the database calls itself
     * @return the dialect
     * @throws IllegalArgumentException when nothing here speaks that database
     */
    public static Dialect of(String product) {
        String written = product == null ? "" : product.toLowerCase();

        if (written.contains("mysql") || written.contains("maria")) {
            return new MySqlDialect();
        }

        if (written.contains("postgres")) {
            return new PostgreSqlDialect();
        }

        throw new IllegalArgumentException(
                ("there is no dialect for '%s' here — this knows mysql and postgresql, and a wrong one "
                 + "would answer rather than fail").formatted(product));
    }

    /**
     * The dialect of whatever this data source is pointed at.
     *
     * <p>⚠️ Opens a connection, which is the point: asked at startup it fails the boot with a sentence,
     * and asked lazily it fails a request. A caller that already knows should say so instead.</p>
     *
     * @param dataSource the data source to ask
     * @return the dialect
     * @throws IllegalStateException when the connection cannot be reached
     */
    public static Dialect of(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return of(connection.getMetaData().getDatabaseProductName());
        } catch (Exception unreachable) {
            throw new IllegalStateException(
                    "the database could not be asked which one it is: " + unreachable.getMessage(),
                    unreachable);
        }
    }
}
