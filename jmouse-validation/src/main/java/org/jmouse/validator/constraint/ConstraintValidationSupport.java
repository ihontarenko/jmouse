package org.jmouse.validator.constraint;

import org.jmouse.validator.Validator;
import org.jmouse.validator.ValidationHints;
import org.jmouse.validator.constraint.adapter.core.ConstraintSchemaValidator;
import org.jmouse.validator.constraint.handler.ConstraintHandler;
import org.jmouse.validator.constraint.handler.SchemaSelector;
import org.jmouse.validator.constraint.processor.ConstraintProcessor;
import org.jmouse.validator.constraint.registry.ConstraintSchemaRegistry;

import java.util.function.Supplier;

/**
 * Single entry point to create a core {@link Validator} backed by constraint schemas.
 */
public final class ConstraintValidationSupport {

    private ConstraintValidationSupport() {}

    public static Validator validator(
            ConstraintSchemaRegistry schemaRegistry,
            SchemaSelector selector,
            Supplier<ValidationHints> hintsSupplier
    ) {
        ConstraintProcessor processor = new ConstraintProcessor();
        ConstraintHandler   handler   = new ConstraintHandler(processor);

        return new ConstraintSchemaValidator(
                schemaRegistry,
                handler,
                selector,
                hintsSupplier::get
        );
    }

    public static Validator validator(
            ConstraintSchemaRegistry schemaRegistry,
            ConstraintHandler handler,
            SchemaSelector selector,
            Supplier<ValidationHints> hintsSupplier
    ) {
        return new ConstraintSchemaValidator(
                schemaRegistry,
                handler,
                selector,
                hintsSupplier::get
        );
    }
}
