package org.jmouse.validator.constraint;

import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

/**
 * SPI contract for contributing constraint types to a {@link ConstraintTypeRegistry}. 🔌
 *
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}
 * and invoked by {@link ConstraintAutoRegistrar}.
 * </p>
 *
 * <p>
 * Typical usage:
 * </p>
 *
 * <pre>{@code
 * public final class DefaultConstraintContributor
 *         implements ConstraintTypeContributor {
 *
 *     @Override
 *     public void contribute(ConstraintTypeRegistry registry) {
 *         registry.register("MinMax", MinMaxConstraint.class);
 *         registry.register("OneOf", OneOfConstraint.class);
 *     }
 * }
 * }</pre>
 *
 * <p>
 * To enable auto-registration, the implementation class must be declared in:
 * </p>
 *
 * <pre>{@code
 * META-INF/services/org.jmouse.validator.constraint.ConstraintTypeContributor
 * }</pre>
 *
 * <p>
 * This SPI keeps constraint type registration modular and
 * avoids reflection-based scanning.
 * </p>
 */
@FunctionalInterface
public interface ConstraintTypeContributor {

    /**
     * Contributes constraint type mappings into the given registry.
     *
     * @param registry target registry
     */
    void contribute(ConstraintTypeRegistry registry);
}