package org.jmouse.core.context;

import java.util.Map;

public record ExecutionSnapshot(Map<ContextKey<?>, Object> entries) { }