package org.jmouse.validator;

import org.jmouse.core.access.PropertyPath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public abstract class AbstractErrors implements Errors {

    private final Object target;
    private final String objectName;

    private final MessageCodesResolver codesResolver;

    private final List<ObjectError> globalErrors = new ArrayList<>();
    private final List<FieldError>  fieldErrors  = new ArrayList<>();

    private final Deque<PropertyPath> stack  = new ArrayDeque<>();
    private       PropertyPath        nested = PropertyPath.empty();

    protected AbstractErrors(Object target, String objectName) {
        this(target, objectName, new SimpleMessageCodesResolver());
    }

    protected AbstractErrors(Object target, String objectName, MessageCodesResolver resolver) {
        this.target = target;
        this.objectName = objectName == null ? "object" : objectName;
        this.codesResolver = resolver == null ? new SimpleMessageCodesResolver() : resolver;
    }

    // ---- Nested path ----

    @Override
    public final PropertyPath getNestedPath() {
        return nested;
    }

    @Override
    public final void pushPath(String segment) {
        if (segment == null || segment.isBlank()) {
            return;
        }
        pushPath(PropertyPath.forPath(segment));
    }

    @Override
    public final void pushPath(PropertyPath segment) {
        if (segment == null || segment.isEmpty()) {
            return;
        }
        stack.push(nested);
        nested = nested.isEmpty() ? segment : nested.append(segment.path());
    }

    @Override
    public final void popPath() {
        if (stack.isEmpty()) {
            nested = PropertyPath.empty();
            return;
        }
        nested = stack.pop();
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

    // ---- Errors ----

    @Override
    public void reject(String errorCode, String defaultMessage, Object... arguments) {
        String[] codes = codesResolver.resolveCodes(objectName, errorCode);
        globalErrors.add(new ObjectError(objectName, codes, defaultMessage, arguments));
    }

    @Override
    public void rejectValue(String field, String errorCode, String defaultMessage, Object... arguments) {
        String   resolvedField = resolveNestedField(field);
        Object   rejected      = tryGetValue(resolvedField);
        String[] codes         = codesResolver.resolveCodes(objectName, resolvedField, errorCode);
        fieldErrors.add(new FieldError(objectName, resolvedField, rejected, codes, defaultMessage, arguments));
    }

    private String resolveNestedField(String field) {
        if (field == null) {
            field = "";
        }

        if (nested == null || nested.isEmpty()) {
            return field;
        }

        if (field.isBlank()) {
            return nested.path();
        }

        // nested + "." + field
        return nested.append(field).path();
    }

    @Override
    public List<FieldError> getErrors() {
        return fieldErrors;
    }

    @Override
    public List<ObjectError> getGlobalErrors() {
        return globalErrors;
    }

    // ---- Optional resolvers ----

    protected Object tryGetValue(String field) {
        return null;
    }

    protected Class<?> tryGetType(String field) {
        return null;
    }

    @Override
    public final Object getFieldValue(String field) {
        return tryGetValue(field);
    }

    @Override
    public final Class<?> getFieldType(String field) {
        return tryGetType(field);
    }
}
