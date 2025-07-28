package org.jmouse.mvc;

import java.util.Map;

public record RoutePath(String pattern, Map<String, Object> variables) {
}
