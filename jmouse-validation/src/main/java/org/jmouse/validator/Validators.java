package org.jmouse.validator;

import org.jmouse.core.Customizer;

import java.util.function.Supplier;

public final class Validators {

    public static <T> Validator fields(Class<T> type, Customizer<FieldRules<T>> customizer) {
        return Validator.forInstance(type, (t, errors) -> {
            FieldRules<T> rules = new FieldRules<>(t, errors);
            customizer.customize(rules);
        });
    }

    public static final class FieldRules<T> {

        private final T      value;
        private final Errors errors;

        FieldRules(T value, Errors errors) {
            this.value = value;
            this.errors = errors;
        }

        public FieldRule field(String path, Supplier<Object> getter) {
            return new FieldRule(path, getter.get(), errors);
        }

    }

    public static final class FieldRule {

        private final String path;
        private final Errors errors;
        private final Object value;

        FieldRule(String path, Object value, Errors errors) {
            this.path = path;
            this.errors = errors;
            this.value = value;
        }

        public FieldRule notBlank(String code, String message) {
            if (!(value instanceof CharSequence sequence) || sequence.toString().isBlank()) {
                errors.rejectValue(path, code, message);
            }
            return this;
        }

        public FieldRule required(String code, String message) {
            if (value == null) {
                errors.rejectValue(path, code, message);
            }
            return this;
        }
    }

}
