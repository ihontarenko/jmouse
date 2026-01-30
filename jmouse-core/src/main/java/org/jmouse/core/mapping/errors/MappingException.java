package org.jmouse.core.mapping.errors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MappingException extends RuntimeException {

    private final String              code;
    private final Map<String, Object> meta;

    public MappingException(String code, String message) {
        this(code, message, null, Map.of());
    }

    public MappingException(String code, String message, Throwable cause) {
        this(code, message, cause, Map.of());
    }

    public MappingException(String code, String message, Throwable cause, Map<String, Object> meta) {
        super(message, cause);
        this.code = code;
        this.meta = Collections.unmodifiableMap(new LinkedHashMap<>(meta));
    }

    public String code() {
        return code;
    }

    public Map<String, Object> meta() {
        return meta;
    }

    public MappingException withMeta(String key, Object value) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(this.meta);
        copy.put(key, value);
        return new MappingException(this.code, getMessage(), getCause(), copy);
    }

    @Override
    public String toString() {
        return "[" + code.toUpperCase() + "]: " + super.toString();
    }

}
