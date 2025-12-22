package org.jmouse.jdbc.database;

import org.jmouse.core.Ordered;

public interface DatabasePlatformProvider extends Ordered {

    /**
     * @return true if this provider supports given database info.
     */
    boolean supports(DatabaseInformation info);

    /**
     * Build a DatabasePlatform instance for the matched database.
     */
    DatabasePlatform create(DatabaseInformation info);

    /**
     * Priority for tie-breaking when multiple providers support the same DB.
     */
    default int order() {
        return 0;
    }
}
