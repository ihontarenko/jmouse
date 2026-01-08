package org.jmouse.core.context;

import static org.jmouse.core.Verify.nonNull;

public final class ContextKey<T> {

    private final String   id;
    private final Class<T> type;

    private ContextKey(String id, Class<T> type) {
        this.id = nonNull(id, "id");
        this.type = nonNull(type, "type");
    }

    public static <T> ContextKey<T> of(String id, Class<T> type) {
        return new ContextKey<>(id, type);
    }

    public String id() {
        return id;
    }

    public Class<T> type() {
        return type;
    }

    @Override
    public String toString() {
        return "ContextKey[%s]".formatted(id);
    }

}
