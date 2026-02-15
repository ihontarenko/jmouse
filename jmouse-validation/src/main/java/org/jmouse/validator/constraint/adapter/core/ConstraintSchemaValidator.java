package org.jmouse.validator.constraint.adapter.core;

import org.jmouse.validator.Errors;
import org.jmouse.validator.Validator;
import org.jmouse.validator.ValidationHints;
import org.jmouse.validator.constraint.handler.ConstraintHandler;
import org.jmouse.validator.constraint.handler.SchemaSelector;
import org.jmouse.validator.constraint.model.ConstraintSchema;
import org.jmouse.validator.constraint.registry.InMemoryConstraintSchemaRegistry;

public final class ConstraintSchemaValidator implements Validator {

    private final InMemoryConstraintSchemaRegistry schemaRegistry;
    private final ConstraintHandler                handler;
    private final SchemaSelector           selector;
    private final ValidationHintsSupplier  hintsSupplier;

    public ConstraintSchemaValidator(
            InMemoryConstraintSchemaRegistry schemaRegistry,
            ConstraintHandler handler,
            SchemaSelector selector,
            ValidationHintsSupplier hintsSupplier
    ) {
        this.schemaRegistry = schemaRegistry;
        this.handler = handler;
        this.selector = selector;
        this.hintsSupplier = hintsSupplier;
    }

    @Override
    public void validate(Object object, Errors errors) {
        if (errors == null) {
            return;
        }

        String          objectName = errors.getObjectName();
        ValidationHints hints      = (hintsSupplier == null) ? ValidationHints.empty() : hintsSupplier.get();
        String          schemaName = selector.select(objectName, hints);

        if (schemaName == null || schemaName.isBlank()) {
            return;
        }

        ConstraintSchema schema = schemaRegistry.resolve(schemaName).orElse(null);

        if (schema == null) {
            return;
        }

        handler.validate(object, schema, errors);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

    @FunctionalInterface
    public interface ValidationHintsSupplier {
        ValidationHints get();
    }
}
