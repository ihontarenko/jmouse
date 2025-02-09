package org.jmouse.validator.jsr380;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import jakarta.validation.metadata.BeanDescriptor;

import java.util.Set;

public class Jsr380Validator implements Validator {

    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        return Set.of();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        return Set.of();
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        return Set.of();
    }

    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return null;
    }

    @Override
    public ExecutableValidator forExecutables() {
        return null;
    }

}
