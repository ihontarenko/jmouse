package org.jmouse.validator.constraint.handler;

import org.jmouse.validator.Hints;

/**
 * Strategy for selecting a validation schema based on context. 🧭
 *
 * <p>
 * {@code SchemaSelector} determines which logical schema name
 * should be applied for a given binding target and validation hints.
 * </p>
 *
 * <p>
 * This allows dynamic schema resolution depending on:
 * </p>
 * <ul>
 *     <li>object name (e.g. form identifier),</li>
 *     <li>operation type (create / update),</li>
 *     <li>user role or custom flags,</li>
 *     <li>other contextual {@link Hints}.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * public final class DefaultSchemaSelector implements SchemaSelector {
 *
 *     @Override
 *     public String select(String objectName, ValidationHints hints) {
 *         if ("userForm".equals(objectName) && hints.isCreate()) {
 *             return "user.create";
 *         }
 *         if ("userForm".equals(objectName) && hints.isUpdate()) {
 *             return "user.update";
 *         }
 *         return null;
 *     }
 * }
 * }</pre>
 *
 * <p>
 * Returning {@code null} indicates that no schema should be applied.
 * </p>
 */
public interface SchemaSelector {

    /**
     * Selects a schema name based on binding context.
     *
     * @param objectName logical binding object name
     *                   (e.g. {@code "userForm"}, {@code "dynamicForm"})
     * @param hints      optional validation hints (may be {@code null})
     * @return schema name to use, or {@code null} if no schema applies
     */
    String select(String objectName, Hints hints);

}