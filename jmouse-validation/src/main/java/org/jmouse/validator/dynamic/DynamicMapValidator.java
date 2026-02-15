package org.jmouse.validator.dynamic;

import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.validator.Errors;
import org.jmouse.validator.Validator;

import java.util.Map;

public final class DynamicMapValidator implements Validator {

    private final ValidationSchema   schema;
    private final DynamicConstraints constraints; // registry of constraint executors

    public DynamicMapValidator(ValidationSchema schema, DynamicConstraints constraints) {
        this.schema = schema;
        this.constraints = constraints;
    }

    @Override
    public void validate(Object object, Errors errors) {
        if (!(object instanceof Map<?, ?> map)) {
            errors.reject("type", "Expected Map");
            return;
        }

        ObjectAccessor accessor = ObjectAccessor.wrapObject(map);

        for (FieldRule rule : schema.rules()) {
            Object value = null;

            try {
                ObjectAccessor objectAccessor = accessor.navigate(rule.path());
                value = (objectAccessor == null ? null : objectAccessor.unwrap());
            } catch (RuntimeException ignore) { }

            for (ConstraintRule constraintRule : rule.constraints()) {
                ConstraintExecutor constraintExecutor = constraints.get(constraintRule.id());

                if (constraintExecutor == null) {
                    errors.rejectValue(rule.path(), "constraint.unknown", "Unknown constraint: " + constraintRule.id());
                    continue;
                }

                boolean ok = constraintExecutor.test(value, constraintRule.arguments());

                if (!ok) {
                    errors.rejectValue(rule.path(), constraintRule.id(), constraintRule.message(), constraintRule.arguments());
                }
            }
        }
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz);
    }
}

