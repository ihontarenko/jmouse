package org.jmouse.validator;

/**
 * Default concrete implementation of {@link AbstractErrors}. 📋
 *
 * <p>
 * Provides a ready-to-use {@link Errors} implementation for
 * standard validation scenarios.
 * </p>
 *
 * <p>
 * Behavior:
 * </p>
 * <ul>
 *     <li>Stores object/global errors and field errors,</li>
 *     <li>Supports nested path stack operations,</li>
 *     <li>Delegates message code resolution to {@link MessageCodesResolver}.</li>
 * </ul>
 *
 * <h3>Typical usage</h3>
 *
 * <pre>{@code
 * UserForm form = ...;
 * Errors errors = new DefaultErrors(form);
 *
 * validator.validate(form, errors);
 *
 * if (errors.hasErrors()) {
 *     // handle validation failure
 * }
 * }</pre>
 *
 * <p>
 * The default {@code objectName} is derived from the target's simple class name,
 * or {@code "object"} if target is {@code null}.
 * </p>
 */
public final class DefaultErrors extends AbstractErrors {

    /**
     * Creates {@link DefaultErrors} with inferred object name.
     *
     * @param target validation target
     */
    public DefaultErrors(Object target) {
        this(target, target == null ? "object" : target.getClass().getSimpleName());
    }

    /**
     * Creates {@link DefaultErrors} with explicit object name.
     *
     * @param target     validation target
     * @param objectName logical object name (used in error codes)
     */
    public DefaultErrors(Object target, String objectName) {
        super(target, objectName);
    }

    /**
     * Creates {@link DefaultErrors} with explicit message code resolver.
     *
     * @param target     validation target
     * @param objectName logical object name
     * @param resolver   message codes resolver
     */
    public DefaultErrors(Object target, String objectName, MessageCodesResolver resolver) {
        super(target, objectName, resolver);
    }

}