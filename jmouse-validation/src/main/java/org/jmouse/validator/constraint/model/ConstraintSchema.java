package org.jmouse.validator.constraint.model;

import java.util.List;

/**
 * Represents a named validation schema composed of field-level rules. 📘
 *
 * <p>
 * A {@code ConstraintSchema} groups multiple {@link FieldRules}
 * under a logical name (e.g. {@code "userForm"}, {@code "registration"},
 * {@code "dynamicForm"}).
 * </p>
 *
 * <p>
 * Schemas are typically:
 * </p>
 * <ul>
 *     <li>Registered in a {@code ConstraintSchemaRegistry},</li>
 *     <li>Selected at runtime via a {@code SchemaSelector},</li>
 *     <li>Applied by a {@code ConstraintHandler}.</li>
 * </ul>
 *
 * <h3>Conceptual structure</h3>
 *
 * <pre>{@code
 * schema "userForm" {
 *     field "age" {
 *         minMax(...)
 *     }
 *     field "role" {
 *         oneOf(...)
 *     }
 * }
 * }</pre>
 *
 * <p>
 * Implementations are expected to be immutable or effectively immutable
 * after construction.
 * </p>
 */
public interface ConstraintSchema {

    /**
     * @return logical schema name (unique within registry)
     */
    String name();

    /**
     * @return ordered list of field-level validation rules
     */
    List<? extends FieldRules> fields();

}