package org.jmouse.validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Base {@link Errors} implementation with in-memory storage for global and field errors. 🧩
 *
 * <p>{@code AbstractErrors} provides:</p>
 * <ul>
 *   <li>storage for {@link ObjectError} (global) and {@link FieldError} (field) errors</li>
 *   <li>Spring-like error code expansion via {@link #getErrorCodes(String)}</li>
 *   <li>optional hooks for resolving rejected field values/types for richer diagnostics</li>
 * </ul>
 *
 * <p>Subclasses may override {@link #tryGetValue(String)} and {@link #tryGetType(String)}
 * to integrate with a binder, accessor, or reflection-based resolver.</p>
 */
public abstract class AbstractErrors implements Errors {

    /**
     * Separator used when composing hierarchical error codes.
     */
    public static final String ERROR_CODE_PATH_SEPARATOR = ".";

    private final Object target;
    private final String objectName;

    private final List<ObjectError> globalErrors = new ArrayList<>();
    private final List<FieldError>  fieldErrors  = new ArrayList<>();

    /**
     * Create an {@link Errors} instance bound to a specific target and logical object name.
     *
     * @param target validated target object (may be {@code null})
     * @param objectName logical object name used in error codes (defaults to {@code "object"} when {@code null})
     */
    protected AbstractErrors(Object target, String objectName) {
        this.target = target;
        this.objectName = objectName == null ? "object" : objectName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getObjectName() {
        return objectName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Object getTarget() {
        return target;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stores an {@link ObjectError} in {@link #getGlobalErrors()}.</p>
     */
    @Override
    public void reject(String errorCode, String defaultMessage, Object... arguments) {
        globalErrors.add(new ObjectError(objectName, getErrorCodes(errorCode), defaultMessage, arguments));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stores a {@link FieldError} in {@link #getErrors()} and attempts to resolve
     * the rejected value using {@link #tryGetValue(String)}.</p>
     */
    @Override
    public void rejectValue(String field, String errorCode, String defaultMessage, Object... arguments) {
        Object rejected = tryGetValue(field);
        fieldErrors.add(new FieldError(objectName, field, rejected, getErrorCodes(errorCode), defaultMessage, arguments));
    }

    /**
     * Build an ordered list of message codes for a given error code.
     *
     * <p>Default chain:</p>
     * <ul>
     *   <li>{@code objectName + "." + code}</li>
     *   <li>{@code code}</li>
     * </ul>
     *
     * @param code base error code
     * @return ordered array of message codes
     */
    @Override
    public String[] getErrorCodes(String code) {
        return new String[]{
                objectName + ERROR_CODE_PATH_SEPARATOR + code,
                code
        };
    }

    /**
     * Return collected field errors.
     *
     * @return mutable list backing this errors instance
     */
    @Override
    public List<FieldError> getErrors() {
        return fieldErrors;
    }

    /**
     * Return collected global (object-level) errors.
     *
     * @return mutable list backing this errors instance
     */
    @Override
    public List<ObjectError> getGlobalErrors() {
        return globalErrors;
    }

    /**
     * Optional field value resolver for richer {@link FieldError} metadata.
     *
     * <p>Called by {@link #rejectValue(String, String, String, Object...)} to obtain the rejected value.</p>
     *
     * @param field field name/path
     * @return rejected value, or {@code null} when not resolvable
     */
    protected Object tryGetValue(String field) {
        return null;
    }

    /**
     * Optional field type resolver for validation logic that depends on runtime/declared field types.
     *
     * @param field field name/path
     * @return field type, or {@code null} when not resolvable
     */
    protected Class<?> tryGetType(String field) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Object getFieldValue(String field) {
        return tryGetValue(field);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Class<?> getFieldType(String field) {
        return tryGetType(field);
    }
}
