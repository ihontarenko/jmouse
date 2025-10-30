package org.jmouse.jdbc._wip.core;

import java.util.List;

/**
 * 🧱 Parsed SQL with '?' and ordered param names.
 */
public record NamedSql(String sql, List<String> names) {

    @Override
    public List<String> names() {
        return List.copyOf(names);
    }

}
