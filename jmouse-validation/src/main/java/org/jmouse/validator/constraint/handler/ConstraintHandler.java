package org.jmouse.validator.constraint.handler;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.core.access.ValueNavigator;
import org.jmouse.util.Arrays;
import org.jmouse.validator.Errors;
import org.jmouse.validator.constraint.api.Constraint;
import org.jmouse.validator.constraint.model.*;
import org.jmouse.validator.constraint.processor.ConstraintProcessor;

/**
 * Central validation handler executing {@link ConstraintSchema}.
 *
 * <p>
 * Resolves field values using {@link ValueNavigator} and evaluates
 * constraints through {@link ConstraintProcessor}. Validation errors
 * are reported to {@link Errors}.
 * </p>
 */
public final class ConstraintHandler {

    /**
     * Executes constraint logic.
     */
    private final ConstraintProcessor processor;

    /**
     * Wraps objects into {@link ObjectAccessor} representations.
     */
    private final AccessorWrapper wrapper;

    /**
     * Navigates property paths on target objects.
     */
    private final ValueNavigator navigator;

    /**
     * Resolves error messages for failed constraints.
     */
    private final ConstraintMessageProvider messageProvider;

    /**
     * Creates handler with default infrastructure.
     *
     * <p>Uses {@link ObjectAccessorWrapper}, default {@link ValueNavigator},
     * and {@link ConstraintMessagePolicy}.</p>
     */
    public ConstraintHandler(ConstraintProcessor processor) {
        this(
                processor,
                new ObjectAccessorWrapper(),
                ValueNavigator.defaultNavigator(),
                new ConstraintMessagePolicy()
        );
    }

    /**
     * Creates handler with custom infrastructure components.
     *
     * @param processor constraint executor
     * @param wrapper accessor wrapper
     * @param navigator property path navigator
     * @param messageProvider message resolver
     */
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

    /**
     * Validates target object using the given schema.
     *
     * <p>
     * For each field rule:
     * </p>
     * <ul>
     *     <li>resolve field value via {@link ValueNavigator}</li>
     *     <li>execute each {@link ConstraintRule}</li>
     *     <li>report failures via {@link Errors#rejectValue(String, String, String)}</li>
     * </ul>
     *
     * @param target validation target
     * @param schema constraint schema
     * @param errors error collector
     */
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

                Object[] arguments = Arrays.concatenate(new Object[]{value}, constraint.arguments());
                String   code      = constraint.code();
                String   message   = messageProvider.provideMessage(constraint, rule.messageOverride());

                errors.rejectValue(field.path(), code, message, arguments);
            }
        }
    }
}