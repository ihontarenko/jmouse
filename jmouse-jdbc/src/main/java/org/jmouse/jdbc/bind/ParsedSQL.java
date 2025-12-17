package org.jmouse.jdbc.bind;

import java.util.List;

public record ParsedSQL(String sql, List<String> parameters) { }
