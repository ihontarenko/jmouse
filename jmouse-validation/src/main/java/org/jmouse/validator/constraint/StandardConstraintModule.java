package org.jmouse.validator.constraint;

import org.jmouse.core.Verify;
import org.jmouse.validator.constraint.constraint.*;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

/**
 * Registers standard built-in constraints into {@link ConstraintTypeRegistry}.
 *
 * <p>Focus: dynamic forms + EL names like {@code @MinMax(...)}.</p>
 */
public final class StandardConstraintModule implements ConstraintTypeContributor {

    @Override
    public void contribute(ConstraintTypeRegistry registry) {
        registerDefaults(registry);
    }

    public static void registerDefaults(ConstraintTypeRegistry registry) {
        Verify.nonNull(registry, "registry");
        registry.register("MinMax", MinMaxConstraint.class)
                .alias("MinMax", "Min")
                .alias("MinMax", "Max")
                .alias("MinMax", "Range")
                .register("oneOf", OneOfConstraint.class)
                .register("required", RequiredConstraint.class)
                .register("notBlank", NotBlankConstraint.class)
                .alias("notBlank", "notEmpty")
                .register("webLink", WebLinkConstraint.class);

    }

    private StandardConstraintModule() {}
}
