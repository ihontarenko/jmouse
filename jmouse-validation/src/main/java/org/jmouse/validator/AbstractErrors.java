package org.jmouse.validator;

import org.jmouse.core.Verify;
import org.jmouse.core.access.PropertyPath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Base {@link Errors} implementation with nested-path support and Spring-like helpers. 🧩
 *
 * <p>Provides:</p>
 * <ul>
 *   <li>storage for global ({@link ObjectError}) and field ({@link FieldError}) errors</li>
 *   <li>nested path stack (push/pop) for building qualified field paths</li>
 *   <li>message code resolution via {@link MessageCodesResolver}</li>
 *   <li>convenience queries: counts, first error, field filtering, and error-code checks</li>
 * </ul>
 *
 * <p>Subclasses may override {@link #tryGetFieldValue(String)} and {@link #tryGetFieldType(String)}
 * to enrich {@link FieldError} with rejected value/type metadata.</p>
 */
public abstract class AbstractErrors implements Errors {

    /**
     * Default logical name used when {@code objectName} is {@code null} or blank.
     */
    public static final String DEFAULT_OBJECT_NAME = "object";

    private final String               objectName;
    private final Object               target;
    private final MessageCodesResolver codesResolver;

    private final List<ObjectError>   objectErrors = new ArrayList<>();
    private final List<FieldError>    fieldErrors  = new ArrayList<>();
    private final Deque<PropertyPath> pathStack    = new ArrayDeque<>();
    private       PropertyPath        nestedPath   = PropertyPath.empty();

    /**
     * Create an errors collector using {@link SimpleMessageCodesResolver}.
     *
     * @param target validated target (may be {@code null})
     * @param objectName logical object name (defaults to {@value #DEFAULT_OBJECT_NAME} when blank)
     */
    protected AbstractErrors(Object target, String objectName) {
        this(target, objectName, new SimpleMessageCodesResolver());
    }

    /**
     * Create an errors collector with a custom {@link MessageCodesResolver}.
     *
     * @param target validated target (may be {@code null})
     * @param objectName logical object name (defaults to {@value #DEFAULT_OBJECT_NAME} when blank)
     * @param resolver message codes resolver (never {@code null})
     */
    protected AbstractErrors(Object target, String objectName, MessageCodesResolver resolver) {
        this.target = target;
        this.codesResolver = Verify.nonNull(resolver, "resolver");
        this.objectName = isBlank(objectName) ? DEFAULT_OBJECT_NAME : objectName;
    }

    /** {@inheritDoc} */
    @Override
    public final String getObjectName() {
        return objectName;
    }

    /** {@inheritDoc} */
    @Override
    public final Object getTarget() {
        return target;
    }

    /** {@inheritDoc} */
    @Override
    public final PropertyPath getNestedPath() {
        return nestedPath;
    }

    /** {@inheritDoc} */
    @Override
    public final void pushPath(String segment) {
        pushPath(PropertyPath.forPath(Verify.nonNull(segment, "segment")));
    }

    /** {@inheritDoc} */
    @Override
    public final void pushPath(PropertyPath segment) {
        if (segment == null || segment.isEmpty()) {
            return;
        }

        pathStack.push(nestedPath);
        nestedPath = nestedPath.isEmpty()
                ? segment
                : nestedPath.append(segment.path());
    }

    /** {@inheritDoc} */
    @Override
    public final void popPath() {
        nestedPath = pathStack.isEmpty()
                ? PropertyPath.empty()
                : pathStack.pop();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stores an {@link ObjectError} in {@link #getObjectErrors()}.</p>
     */
    @Override
    public void reject(String errorCode, String defaultMessage, Object... arguments) {
        String[] codes = codesResolver.resolveCodes(objectName, errorCode);
        objectErrors.add(new ObjectError(objectName, codes, defaultMessage, arguments));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stores a {@link FieldError} in {@link #getFieldErrors()} and prefixes the field with
     * {@link #getNestedPath()} when it is not empty.</p>
     */
    @Override
    public void rejectValue(String field, String errorCode, String defaultMessage, Object... arguments) {
        String resolvedField = resolveNestedField(field);

        Object   rejected = tryGetFieldValue(resolvedField);
        String[] codes    = codesResolver.resolveCodes(objectName, resolvedField, errorCode);

        fieldErrors.add(new FieldError(objectName, resolvedField, rejected, codes, defaultMessage, arguments));
    }

    /**
     * Path-aware overload for field errors.
     *
     * @param field field path (may be {@code null})
     * @param errorCode error code
     * @param defaultMessage fallback message (may be {@code null})
     * @param arguments optional message arguments
     */
    @Override
    public void rejectValue(PropertyPath field, String errorCode, String defaultMessage, Object... arguments) {
        rejectValue(field == null ? "" : field.path(), errorCode, defaultMessage, arguments);
    }

    /**
     * Object-level (global) errors.
     *
     * @return mutable list backing this instance
     */
    public List<ObjectError> getObjectErrors() {
        return objectErrors;
    }

    /**
     * Field-level errors.
     *
     * @return mutable list backing this instance
     */
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    /** {@inheritDoc} */
    @Override
    public final List<FieldError> getErrors() {
        return getFieldErrors();
    }

    /** {@inheritDoc} */
    @Override
    public final List<ObjectError> getGlobalErrors() {
        return getObjectErrors();
    }

    /**
     * @return {@code true} if any errors exist (global or field)
     */
    public final boolean hasAnyErrors() {
        return hasFieldErrors() || hasGlobalErrors();
    }

    /**
     * @return {@code true} if at least one field error exists
     */
    public final boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }

    /**
     * @return {@code true} if at least one global error exists
     */
    public final boolean hasGlobalErrors() {
        return !objectErrors.isEmpty();
    }

    /**
     * @return total number of recorded errors (field + global)
     */
    public final int getErrorCount() {
        return fieldErrors.size() + objectErrors.size();
    }

    /**
     * @return number of recorded field errors
     */
    public final int getFieldErrorCount() {
        return fieldErrors.size();
    }

    /**
     * @return number of recorded global errors
     */
    public final int getGlobalErrorCount() {
        return objectErrors.size();
    }

    /**
     * Return the first field error for the given field, or {@code null} if none exist.
     *
     * @param field field name/path (relative, without nested prefix)
     * @return first field error, or {@code null}
     */
    public final FieldError getFieldError(String field) {
        List<FieldError> errors = getFieldErrors(field);
        return errors.isEmpty() ? null : errors.getFirst();
    }

    /**
     * Return all field errors for the given field name.
     *
     * <p>Note: this method matches recorded field names exactly. If you want to search
     * against nested path-qualified fields, pass the full resolved field path.</p>
     *
     * @param field field name/path
     * @return immutable list of matching errors
     */
    public final List<FieldError> getFieldErrors(String field) {
        String f = field == null ? "" : field;
        return fieldErrors.stream()
                .filter(e -> e.getField().equals(f))
                .toList();
    }

    /**
     * @param field field name/path
     * @return {@code true} if any field errors exist for {@code field}
     */
    public final boolean hasFieldErrors(String field) {
        return !getFieldErrors(field).isEmpty();
    }

    /**
     * @return first global error or {@code null}
     */
    public final ObjectError getGlobalError() {
        return objectErrors.isEmpty() ? null : objectErrors.getFirst();
    }

    /**
     * Check whether any recorded error (global or field) contains the given message code.
     *
     * @param code message code to look for
     * @return {@code true} if at least one error contains the code
     */
    public final boolean hasErrorCode(String code) {
        if (isBlank(code)) {
            return false;
        }

        for (ObjectError e : objectErrors) {
            if (containsCode(e, code)) {
                return true;
            }
        }

        for (FieldError e : fieldErrors) {
            if (containsCode(e, code)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check whether any field error for {@code field} contains the given message code.
     *
     * @param field field name/path
     * @param code message code to look for
     * @return {@code true} if at least one matching field error contains the code
     */
    public final boolean hasFieldErrorCode(String field, String code) {
        if (isBlank(code)) {
            return false;
        }

        for (FieldError e : getFieldErrors(field)) {
            if (containsCode(e, code)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Attempt to resolve rejected field value (for {@link FieldError#getRejectedValue()}).
     *
     * @param field resolved field path
     * @return rejected field value, or {@code null} if unavailable
     */
    protected Object tryGetFieldValue(String field) {
        return null;
    }

    /**
     * Attempt to resolve field type for diagnostics.
     *
     * @param field resolved field path
     * @return field type, or {@code null} if unavailable
     */
    protected Class<?> tryGetFieldType(String field) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public final Object getFieldValue(String field) {
        return tryGetFieldValue(field);
    }

    /** {@inheritDoc} */
    @Override
    public final Class<?> getFieldType(String field) {
        return tryGetFieldType(field);
    }

    private String resolveNestedField(String field) {
        String f = field == null ? "" : field;

        if (nestedPath == null || nestedPath.isEmpty()) {
            return f;
        }
        if (f.isBlank()) {
            return nestedPath.path();
        }

        return nestedPath.append(f).path();
    }

    private static boolean containsCode(ObjectError error, String code) {
        String[] codes = error.getCodes();

        if (codes == null) {
            return false;
        }

        for (String c : codes) {
            if (code.equals(c)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
