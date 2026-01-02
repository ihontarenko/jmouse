package org.jmouse.jdbc.query;

import java.util.List;

public record Page<T>(long offset, int limit, List<T> items) {
}
