package org.jmouse.jdbc.operation;

/**
 * Typed SQL mutation operation.
 *
 * <p>This contract marks an operation that modifies database state, such as
 * {@code insert}, {@code update}, or {@code delete}. The execution result is
 * typically the affected row count.</p>
 *
 * <p>The operation itself does not expose SQL text or parameter binding.
 * These concerns are resolved externally.</p>
 *
 * @author Ivan Hontarenko
 */
public interface SqlUpdate extends SqlOperation {
}