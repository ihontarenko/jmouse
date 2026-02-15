package org.jmouse.validator.constraint.handler;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.core.access.ValueNavigator;
import org.jmouse.validator.Errors;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.*;
import org.jmouse.validator.constraint.processor.ConstraintProcessor;

public final class ConstraintHandler {

    private final ConstraintProcessor     processor;
    private final AccessorWrapper         wrapper;
    private final ValueNavigator          navigator;
    private final ConstraintMessagePolicy messagePolicy;

    public ConstraintHandler(ConstraintProcessor processor) {
        this(
                processor,
                new ObjectAccessorWrapper(),
                ValueNavigator.defaultNavigator(),
                new ConstraintMessagePolicy()
        );
    }

    public ConstraintHandler(
            ConstraintProcessor processor,
            AccessorWrapper wrapper,
            ValueNavigator navigator,
            ConstraintMessagePolicy messagePolicy
    ) {
        this.processor = processor;
        this.wrapper = wrapper;
        this.navigator = navigator;
        this.messagePolicy = messagePolicy;
    }

    public void validate(Object target, ConstraintSchema schema, Errors errors) {
        if (schema == null || errors == null) {
            return;
        }

        ObjectAccessor accessor = wrapper.wrap(target);

        for (FieldRules field : schema.fields()) {
            Object value = navigator.navigate(accessor, field.propertyPath().path());

            for (ConstraintRule rule : field.rules()) {
                Constraint constraint = rule.constraint();

                if (processor.test(value, constraint)) {
                    continue;
                }

                String code = constraint.code();

                String message =
                        (rule.messageOverride() != null && !rule.messageOverride().isBlank())
                                ? rule.messageOverride()
                                : messagePolicy.message(constraint);

                errors.rejectValue(field.path(), code, message);
            }
        }
    }
}
