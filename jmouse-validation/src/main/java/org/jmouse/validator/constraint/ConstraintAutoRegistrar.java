package org.jmouse.validator.constraint;

import org.jmouse.core.Verify;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

import java.util.ServiceLoader;

/**
 * Loads {@link ConstraintTypeContributor} via {@link ServiceLoader} and applies them to registry.
 *
 * <p>Zero reflection scanning, stable, modular, Spring-like in spirit.</p>
 */
public final class ConstraintAutoRegistrar {

    private ConstraintAutoRegistrar() {}

    public static void registerAll(ConstraintTypeRegistry registry) {
        Verify.nonNull(registry, "registry");

        ServiceLoader<ConstraintTypeContributor> loader =
                ServiceLoader.load(ConstraintTypeContributor.class);

        for (ConstraintTypeContributor contributor : loader) {
            contributor.contribute(registry);
        }
    }
}
