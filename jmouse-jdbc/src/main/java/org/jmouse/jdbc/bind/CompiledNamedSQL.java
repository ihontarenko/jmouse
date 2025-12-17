package org.jmouse.jdbc.bind;

import java.util.List;

public record CompiledNamedSQL(
        String originalSql,
        String sql,
        List<String> parameters
) { }