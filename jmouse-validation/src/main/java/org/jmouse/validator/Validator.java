package org.jmouse.validator;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface Validator {

    static <T> Validator forInstance(Class<T> type, BiConsumer<T, Errors> validator) {
        return new Typed<>(type, type::isAssignableFrom, validator);
    }

    void validate(Object object, Errors errors);

    boolean supports(Class<?> clazz);

    class Typed<T> implements Validator {

        private final Class<T>              type;
        private final Predicate<Class<?>>   predicate;
        private final BiConsumer<T, Errors> validator;

        public Typed(Class<T> type, Predicate<Class<?>> predicate, BiConsumer<T, Errors> validator) {
            this.type = type;
            this.predicate = predicate;
            this.validator = validator;
        }

        @Override
        public void validate(Object object, Errors errors) {
            validator.accept(type.cast(object), errors);
        }

        @Override
        public boolean supports(Class<?> clazz) {
            return predicate.test(clazz);
        }

    }

}
