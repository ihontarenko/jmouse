package org.jmouse.jdbc.core;

import java.util.List;

public record Batch(String sql, List<Object[]> arguments) { }
