package org.jmouse.validator.constraint.adapter.core;

import org.jmouse.validator.Errors;
import org.jmouse.validator.Validator;
import org.jmouse.validator.Hints;
import org.jmouse.validator.constraint.handler.ConstraintHandler;
import org.jmouse.validator.constraint.handler.SchemaSelector;
import org.jmouse.validator.constraint.model.ConstraintSchema;
import org.jmouse.validator.constraint.registry.ConstraintSchemaRegistry;

/**
 * {@link Validator} implementation that applies a {@link ConstraintSchema}
 * resolved dynamically via {@link SchemaSelector}. 🧠
 *
 * <p>
 * This validator bridges the generic validation infrastructure
 * ({@link Validator}, {@link Errors}) with the constraint-schema engine.
 * </p>
 *
 * <h3>Execution flow</h3>
 * <ol>
 *     <li>Resolve object name from {@link Errors}.</li>
 *     <li>Obtain {@link Hints} from {@link ValidationHintsSupplier}.</li>
 *     <li>Ask {@link SchemaSelector} for schema name.</li>
 *     <li>Resolve {@link ConstraintSchema} from {@link ConstraintSchemaRegistry}.</li>
 *     <li>Delegate validation to {@link ConstraintHandler}.</li>
 * </ol>
 *
 * <p>
 * If any step fails (no schema name, schema not found, etc.),
 * validation is silently skipped.
 * </p>
 *
 * <h3>Example configuration</h3>
 *
 * <pre>{@code
 * ConstraintSchemaValidator validator =
 *     new ConstraintSchemaValidator(
 *         schemaRegistry,
 *         constraintHandler,
 *         schemaSelector,
 *         ValidationHints::empty
 *     );
 * }</pre>
 */
public final class ConstraintSchemaValidator implements Validator {

    private final ConstraintSchemaRegistry schemaRegistry;
    private final ConstraintHandler        handler;
    private final SchemaSelector           selector;
    private final ValidationHintsSupplier  hintsSupplier;

    /**
     * Creates a schema-based validator.
     *
     * @param schemaRegistry registry of available schemas
     * @param handler        constraint execution handler
     * @param selector       schema selector strategy
     * @param hintsSupplier  supplier of validation hints (nullable)
     */
    public ConstraintSchemaValidator(
            ConstraintSchemaRegistry schemaRegistry,
            ConstraintHandler handler,
            SchemaSelector selector,
            ValidationHintsSupplier hintsSupplier
    ) {
        this.schemaRegistry = schemaRegistry;
        this.handler = handler;
        this.selector = selector;
        this.hintsSupplier = hintsSupplier;
    }

    /**
     * Performs validation using a dynamically selected schema.
     *
     * <p>
     * If no schema can be resolved, the method exits without errors.
     * </p>
     *
     * @param object object to validate
     * @param errors error collector (may be {@code null})
     */
    @Override
    public void validate(Object object, Errors errors) {
        if (errors == null) {
            return;
        }

        String          objectName = errors.getObjectName();
        Hints hints =
                (hintsSupplier == null)
                        ? Hints.empty()
                        : hintsSupplier.get();

        String schemaName = selector.select(objectName, hints);

        if (schemaName == null || schemaName.isBlank()) {
            return;
        }

        ConstraintSchema schema =
                schemaRegistry.resolve(schemaName).orElse(null);

        if (schema == null) {
            return;
        }

        handler.validate(object, schema, errors);
    }

    /**
     * Supports all classes.
     *
     * <p>
     * Schema applicability is controlled dynamically
     * by {@link SchemaSelector}.
     * </p>
     */
    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

    /**
     * Supplies {@link Hints} at validation time. 🎯
     *
     * <p>
     * Useful for injecting request-scoped hints such as
     * operation type (create/update) or role context.
     * </p>
     */
    @FunctionalInterface
    public interface ValidationHintsSupplier {

        /**
         * @return current validation hints (never {@code null})
         */
        Hints get();
    }

}