package org.jmouse.validator;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Defines a contract for bean validation.
 * <p>
 * Implementations of this interface provide a way to validate objects
 * and collect validation errors. A {@link Validator} can support
 * specific types of objects and check if a given class is supported.
 * </p>
 *
 * @see Errors
 */
public interface Validator {

    /**
     * Creates a {@link Validator} for a specific type.
     * <p>
     * This method simplifies the creation of type-safe validators by
     * providing a {@link BiConsumer} that performs validation and
     * collects errors.
     * </p>
     *
     * @param type      the class type that this validator supports
     * @param validator a function that validates instances of the specified type
     * @param <T>       the type of the bean being validated
     * @return a new instance of {@link Validator} specialized for the given type
     */
    static <T> Validator forInstance(Class<T> type, BiConsumer<T, Errors> validator) {
        return new Typed<>(type, type::isAssignableFrom, validator);
    }

    /**
     * Validates the given bean and returns a new {@link Errors} instance
     * containing any validation errors.
     * <p>
     * This method provides a convenient way to validate an bean and
     * collect errors without manually creating an {@link Errors} instance.
     * </p>
     *
     * @param value the bean to validate
     * @return an {@link Errors} instance containing validation errors, if any
     */
    default Errors validate(Object value) {
        Errors errors = new DefaultErrors(value);
        validate(value, errors);
        return errors;
    }

    /**
     * Validates the given bean and collects validation errors.
     * <p>
     * If the bean does not meet the validation criteria, errors
     * should be added to the provided {@link Errors} instance.
     * </p>
     *
     * @param object the bean to validate
     * @param errors the errors bean to collect validation issues
     */
    void validate(Object object, Errors errors);

    /**
     * Determines whether this validator supports validation of the given class.
     *
     * @param clazz the class to check
     * @return {@code true} if this validator supports the class, otherwise {@code false}
     */
    boolean supports(Class<?> clazz);

    /**
     * A type-specific implementation of {@link Validator}.
     * <p>
     * This implementation ensures that only instances of a specific type
     * are validated, using a predicate to check if a given class is supported.
     * </p>
     *
     * @param <T> the type of bean being validated
     */
    class Typed<T> implements Validator {

        private final Class<T>              type;
        private final Predicate<Class<?>>   predicate;
        private final BiConsumer<T, Errors> validator;

        /**
         * Constructs a new {@code Typed} validator.
         *
         * @param type      the class type that this validator supports
         * @param predicate a predicate that determines if the validator supports a given class
         * @param validator a function that performs validation on instances of the specified type
         */
        public Typed(Class<T> type, Predicate<Class<?>> predicate, BiConsumer<T, Errors> validator) {
            this.type = type;
            this.predicate = predicate;
            this.validator = validator;
        }

        /**
         * Validates the given bean by casting it to the expected type
         * and applying the provided validation function.
         *
         * @param object the bean to validate
         * @param errors the errors bean to collect validation issues
         */
        @Override
        public void validate(Object object, Errors errors) {
            // todo: Error.nullObjectRejection(errors);
            validator.accept(type.cast(object), errors);
        }

        /**
         * Checks if this validator supports the given class based on the predicate.
         *
         * @param clazz the class to check
         * @return {@code true} if the validator supports the class, otherwise {@code false}
         */
        @Override
        public boolean supports(Class<?> clazz) {
            return predicate.test(clazz);
        }
    }
}
