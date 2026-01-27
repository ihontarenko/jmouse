package org.jmouse.core.mapping.errors;

public final class MappingException extends RuntimeException {

    private final String code;

    public MappingException(String code, String message) {
        this(code, message, null);
    }

    public MappingException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }

}
