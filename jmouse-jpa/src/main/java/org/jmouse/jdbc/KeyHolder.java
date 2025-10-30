package org.jmouse.jdbc;

import java.util.Optional;

/**
 * 🔑 Holder for generated keys (single-column primary keys by default).
 */
public interface KeyHolder {

    Optional<Object> getKey();

    void setKey(Object key);

    <T> Optional<T> getKeyAs(Class<T> type);

}