package org.jmouse.validator.old;

public class ValidationException extends RuntimeException {

    private final String       pointer;
    private final ErrorCode    code;
    private final ErrorContext context;

    public ValidationException(String pointer, String message, Throwable cause, ErrorCode code, ErrorContext context) {
        super(message, cause);

        this.context = context;
        this.code = code;
        this.pointer = pointer;
    }

    public ValidationException(ErrorCode code) {
        this(null, null, null, code, null);
    }

    public ValidationException(ErrorCode code, String message) {
        this(null, message, null, code, null);
    }

    public ValidationException(ErrorCode code, ErrorContext context) {
        this(null, null, null, code, context);
    }

    public ValidationException(String pointer, ErrorCode code, String message) {
        this(pointer, message, null, code, null);
    }

    public ValidationException(String pointer, ErrorCode code, ErrorContext context) {
        this(pointer, null, null, code, context);
    }

    public String getPointer() {
        return pointer;
    }

    public ErrorCode getCode() {
        return code;
    }

    public ErrorContext getContext() {
        return context;
    }
}

