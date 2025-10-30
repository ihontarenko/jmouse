package org.jmouse.jdbc;

import java.util.Collections;
import java.util.Map;

/**
 * 🧾 Parameter source contract for named params.
 */
public interface SqlParameterSource {

    static SqlParameterSource of(Map<String, ?> map) {
        return new MapSqlParameterSource(map);
    }

    static SqlParameterSource empty() {
        return new MapSqlParameterSource(Collections.emptyMap());
    }

    boolean has(String name);

    Object get(String name);

}
