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

        // ---- numeric/range
        registry.register("MinMax", MinMaxConstraint.class);
        registry.alias("MinMax", "Min");
        registry.alias("MinMax", "Max");
        registry.alias("MinMax", "Range");


        // ---- enum/allowed values
        registry.register("oneof", OneOfConstraint.class);
        registry.register("oneOf", OneOfConstraint.class);

        // ---- null/blank
//        registry.register("notnull",  NotNullConstraint.class);
//        registry.register("notNull",  NotNullConstraint.class);
//
//        registry.register("notblank", NotBlankConstraint.class);
//        registry.register("notBlank", NotBlankConstraint.class);
//
//        // ---- size/length
//        registry.register("size",   SizeConstraint.class);
//        registry.register("length", SizeConstraint.class);
//
//        // ---- pattern/email
//        registry.register("pattern", PatternConstraint.class);
//        registry.register("email",   EmailConstraint.class);
//
//        // ---- numeric sign
//        registry.register("positive", PositiveConstraint.class);
    }

    private StandardConstraintModule() {}
}
