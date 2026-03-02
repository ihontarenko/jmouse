package org.jmouse.validator.constraint;

import org.jmouse.core.Verify;
import org.jmouse.validator.constraint.registry.ConstraintTypeRegistry;

import java.util.ServiceLoader;

/**
 * Auto-registers constraint types using {@link ServiceLoader}. 🧩
 *
 * <p>
 * {@code ConstraintAutoRegistrar} discovers {@link ConstraintTypeContributor}
 * implementations declared via {@code META-INF/services} and lets them
 * contribute mappings into a {@link ConstraintTypeRegistry}.
 * </p>
 *
 * <p>
 * This approach provides a modular, zero-scanning mechanism similar in spirit
 * to Spring's auto-configuration:
 * </p>
 * <ul>
 *     <li>✅ no classpath reflection scanning</li>
 *     <li>✅ works well with multi-module builds</li>
 *     <li>✅ explicit and stable registration via service files</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * ConstraintTypeRegistry registry = new InMemoryConstraintTypeRegistry();
 * ConstraintAutoRegistrar.registerAll(registry);
 * }</pre>
 *
 * <h3>ServiceLoader wiring</h3>
 * <p>
 * Provide a file:
 * </p>
 * <pre>{@code
 * META-INF/services/org.jmouse.validator.constraint.ConstraintTypeContributor
 * }</pre>
 * <p>
 * With implementation class names, one per line:
 * </p>
 * <pre>{@code
 * com.example.validation.MyConstraintContributor
 * }</pre>
 */
public final class ConstraintAutoRegistrar {

    private ConstraintAutoRegistrar() {}

    /**
     * Loads all {@link ConstraintTypeContributor} services and applies them to the registry.
     *
     * @param registry target registry to populate
     */
    public static void registerAll(ConstraintTypeRegistry registry) {
        Verify.nonNull(registry, "registry");

        ServiceLoader<ConstraintTypeContributor> loader =
                ServiceLoader.load(ConstraintTypeContributor.class);

        for (ConstraintTypeContributor contributor : loader) {
            contributor.contribute(registry);
        }
    }
}