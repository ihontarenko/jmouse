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

    private final ConstraintProcessor       processor;
    private final AccessorWrapper           wrapper;
    private final ValueNavigator            navigator;
    private final ConstraintMessageProvider messageProvider;

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
            ConstraintMessageProvider messageProvider
    ) {
        this.processor = processor;
        this.wrapper = wrapper;
        this.navigator = navigator;
        this.messageProvider = messageProvider;
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

                String code    = constraint.code();
                String message = messageProvider.provideMessage(constraint, rule.messageOverride());

                errors.rejectValue(field.path(), code, message);
            }
        }
    }
}
