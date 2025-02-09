package org.jmouse.validator.manual;

import org.jmouse.common.support.objects.BeanObjectInfo;
import org.jmouse.common.support.objects.FieldAccessor;

import java.util.*;
import java.util.function.Consumer;

public class Validation {

    private final List<Validator>              globalValidators = new ArrayList<>();
    private final Map<String, List<Validator>> fieldsValidators = new HashMap<>();
    private final String                       name;

    public Validation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addValidator(Validator validator) {
        this.globalValidators.add(validator);
    }

    public void addValidator(String fieldName, Validator validator) {
        this.fieldsValidators.computeIfAbsent(fieldName, key -> new ArrayList<>()).add(validator);
    }

    @SuppressWarnings({"all"})
    public Errors validate(Object object, ValidationContext context) {
        Errors errors = new Errors();

        context.setAttribute(ValidationContext.VALIDATION_MANAGER_KEY, this);

        for (Validator validator : globalValidators) {
            applyValidator(object, validator, context, errors);
        }

        return errors;
    }

    public void validate(BeanObjectInfo beanInfo, String fieldName, Errors errors, ValidationContext context) {
        context.setAttribute(ValidationContext.VALIDATION_MANAGER_KEY, this);

        FieldAccessor   accessor   = beanInfo.getBeanField(fieldName).getFieldAccessor();
        Object          value      = accessor.getValue();
        List<Validator> validators = fieldsValidators.getOrDefault(fieldName, Collections.emptyList());

        context.setPointer(fieldName);

        for (Validator validator : validators) {
            applyValidator(value, validator, context, errors);
        }

        context.setPointer(null);
    }

    public Errors validate(BeanObjectInfo beanInfo, Iterable<String> fieldNames, ValidationContext context) {
        Errors errors = new Errors();

        for (String fieldName : fieldNames) {
            validate(beanInfo, fieldName, errors, context);
        }

        return errors;
    }

    private void applyValidator(Object object, Validator validator, ValidationContext context, Errors errors) {
        if (validator.supports(object)) {
            try {
                validator.validate(object, errors, context);
            } catch (ValidationException exception) {
                errors.rejectValue(exception.getPointer(), exception.getMessage(), exception.getCode().name());
            }
        }
    }

    public void configure(Consumer<Validation> consumer) {
        consumer.accept(this);
    }

}
