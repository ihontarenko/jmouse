package org.jmouse.core.context.result;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🗂 Map-backed mutable result context.
 *
 * <p>
 * Default {@link MutableResultContext} implementation
 * storing errors by <b>error code</b> and a single
 * execution return value.
 * </p>
 *
 * <p>
 * Preserves insertion order of errors and enforces
 * uniqueness per error code.
 * </p>
 */
public final class MapBackedMutableResultContext implements MutableResultContext {

    /**
     * ❌ Errors indexed by error code.
     */
    private final Map<String, ErrorDetails> errors;

    /**
     * 📦 Execution return value.
     */
    private Object returnValue;

    /**
     * 🏗 Create empty result context.
     */
    public MapBackedMutableResultContext() {
        this.errors = new LinkedHashMap<>();
    }

    /**
     * 📦 Get return value.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getReturnValue() {
        return (T) returnValue;
    }

    /**
     * 📦 Set return value.
     */
    @Override
    public void setReturnValue(Object value) {
        this.returnValue = value;
    }

    /**
     * ❌ Check error presence.
     */
    @Override
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 📋 Get all errors (read-only).
     */
    @Override
    public Iterable<ErrorDetails> getErrors() {
        return Collections.unmodifiableCollection(errors.values());
    }

    /**
     * 🔎 Get error by code.
     */
    @Override
    public ErrorDetails getError(String code) {
        return errors.get(code);
    }

    /**
     * ❌ Add or replace error by code.
     */
    @Override
    public void addError(ErrorDetails errorDetails) {
        errors.put(errorDetails.code(), errorDetails);
    }

    /**
     * 🧹 Clear return value and errors.
     */
    @Override
    public void clear() {
        errors.clear();
        returnValue = null;
    }

    /**
     * 🧾 Debug-friendly representation.
     */
    @Override
    public String toString() {
        return "ResultContext[returnValue=" + returnValue + ", errors=" + errors + "]";
    }
}
