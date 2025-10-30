package org.jmouse.jdbc._wip.core;

import java.util.List;

public record Batch(String sql, List<Object[]> arguments) { }
