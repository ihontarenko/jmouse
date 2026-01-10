package org.jmouse.core.scope;

import java.util.HashMap;
import java.util.Map;

abstract public class AbstractResultContext implements ResultContext {

    private final Map<String, ErrorDetails> errors;
    private       Object                    value;

    public AbstractResultContext() {
        this.errors = new HashMap<>();
    }

    @Override
    public <T> T getReturnValue() {
        return (T) value;
    }

    @Override
    public void setReturnValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean hasErrors() {
        return errors.size() > 0;
    }

    @Override
    public Iterable<ErrorDetails> getErrors() {
        return errors.values();
    }

    @Override
    public ErrorDetails getError(String name) {
        return errors.get(name);
    }

    @Override
    public void addError(ErrorDetails errorDetails) {
        errors.put(errorDetails.code(), errorDetails);
    }

    @Override
    public void addError(String code, String message) {
        addError(new ErrorDetails(code, message));
    }

    @Override
    public void cleanup() {
        errors.clear();
        value = null;
    }
}
