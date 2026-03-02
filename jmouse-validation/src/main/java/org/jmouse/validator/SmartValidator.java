package org.jmouse.validator;

/**
 * Extended {@link Validator} contract supporting contextual {@link Hints}. 🧠
 *
 * <p>
 * {@code SmartValidator} allows passing additional runtime metadata
 * into validation logic without modifying the {@link Validator} API.
 * </p>
 *
 * <p>
 * Hints can be used for:
 * </p>
 * <ul>
 *     <li>Operation mode (create/update)</li>
 *     <li>Validation groups</li>
 *     <li>Role-based validation</li>
 *     <li>Feature flags</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * SmartValidator validator = ...
 *
 * Hints hints = Hints.of(Operation.CREATE);
 *
 * validator.validate(user, errors, hints);
 * }</pre>
 *
 * <p>
 * The default {@link #validate(Object, Errors)} method delegates
 * to {@link #validate(Object, Errors, Hints)} with {@link Hints#empty()}.
 * </p>
 */
public interface SmartValidator extends Validator {

    /**
     * Validates the target object using additional contextual hints.
     *
     * @param target object to validate
     * @param errors error collector
     * @param hints  contextual hints (never {@code null})
     */
    void validate(Object target, Errors errors, Hints hints);

    /**
     * Default validation without hints.
     *
     * <p>
     * Delegates to {@link #validate(Object, Errors, Hints)} using
     * {@link Hints#empty()}.
     * </p>
     */
    @Override
    default void validate(Object object, Errors errors) {
        validate(object, errors, Hints.empty());
    }
}