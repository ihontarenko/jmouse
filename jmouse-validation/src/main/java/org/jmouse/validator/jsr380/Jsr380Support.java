package org.jmouse.validator.jsr380;

import org.jmouse.core.Verify;
import org.jmouse.validator.ValidatorRegistry;

/**
 * Convenience integration helpers for registering JSR-380 validators into a {@link ValidatorRegistry}. 🔗
 *
 * <p>This class simplifies bootstrapping Jakarta Bean Validation support in the jMouse validation layer
 * by creating and registering {@link Jsr380ValidatorAdapter} instances.</p>
 *
 * <p>Two registration modes are supported:</p>
 * <ul>
 *   <li>Default adapter using standard Jakarta validation configuration</li>
 *   <li>Adapter with custom {@link Jsr380ViolationMapper}</li>
 * </ul>
 */
public final class Jsr380Support {

    private Jsr380Support() {
    }

    /**
     * Register a default JSR-380 validator adapter into the registry.
     *
     * <p>Internally creates a default {@link Jsr380ValidatorAdapter} using
     * {@link Jsr380Validators#createDefaultAdapter()}.</p>
     *
     * @param registry validator registry (must not be {@code null})
     */
    public static void registerInto(ValidatorRegistry registry) {
        Verify.nonNull(registry, "registry")
                .register(Jsr380Validators.createDefaultAdapter());
    }

    /**
     * Register a JSR-380 validator adapter using a custom violation mapper.
     *
     * <p>Internally creates an adapter using
     * {@link Jsr380Validators#createDefaultAdapter(Jsr380ViolationMapper)}.</p>
     *
     * @param registry validator registry (must not be {@code null})
     * @param mapper violation mapper used to translate constraint violations
     */
    public static void registerInto(ValidatorRegistry registry, Jsr380ViolationMapper mapper) {
        Verify.nonNull(registry, "registry")
                .register(Jsr380Validators.createDefaultAdapter(mapper));
    }
}
