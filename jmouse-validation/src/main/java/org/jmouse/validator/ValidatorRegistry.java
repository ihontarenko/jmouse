package org.jmouse.validator;

public interface ValidatorRegistry {

    /**
     * 🧩 Register a validator (order matters).
     */
    void register(Validator validator);

    /**
     * 🔎 Validate target with all validators that support its type.
     */
    void validate(Object target, Errors errors);
}
