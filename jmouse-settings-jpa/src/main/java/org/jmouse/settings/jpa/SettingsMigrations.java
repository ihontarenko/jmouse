package org.jmouse.settings.jpa;

import org.jmouse.jdbc.dialect.Dialects;

import javax.sql.DataSource;

/**
 * 🚚 Where the library's own schema lives, and under what history: its own table, so adopting it costs a dependency rather than a
 * negotiation with a product's numbering. The folder is the dialect's name ({@code db/settings/mysql}), asked of the connection
 * through {@link Dialects}, never guessed.
 *
 * <p><strong>Rewritten, not altered, while every database it runs on may be dropped</strong> (Ivan, 2026-11-14: «базу можна
 * обнуляти, щоб не робити альтери»): a change of shape or of what a key looks like is the migration rewritten to its final state
 * and the settings tables built anew. Append-only from the first release whose data somebody else keeps.</p>
 */
public final class SettingsMigrations {

    /** History table for the library's migrations, apart from every product's own. */
    public static final String HISTORY_TABLE = "settings_schema_history";

    /** Classpath root the per-dialect folders sit under. */
    public static final String LOCATION_ROOT = "db/settings";

    /** Bean name of the migrator, so a product's own Flyway is ordered after it by name. */
    public static final String MIGRATOR_BEAN_NAME = "settingsFlywayMigrator";

    private SettingsMigrations() {
    }

    /** The migration location for whatever database a data source speaks: {@code classpath:db/settings/mysql}. */
    public static String locationFor(DataSource dataSource) {
        return "classpath:" + LOCATION_ROOT + "/" + Dialects.of(dataSource).name();
    }
}
