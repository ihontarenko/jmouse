package org.jmouse.jdbc.query;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.database.DatabasePlatform;
import org.jmouse.jdbc.database.SQLTemplates;

public class SimpleSQLTemplates implements SQLTemplates {

    private final DatabasePlatform platform;

    public SimpleSQLTemplates(DatabasePlatform platform) {
        this.platform = Contract.nonNull(platform, "platform");
    }

    @Override
    public String limitOffset(String sql, int offset, int limit) {
        return platform.pagination().apply(sql, OffsetLimit.of(offset, limit));
    }
    
}