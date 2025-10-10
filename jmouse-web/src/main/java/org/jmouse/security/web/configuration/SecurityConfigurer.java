package org.jmouse.security.web.configuration;

/**
 * ⚙️ {@code SecurityConfigurer}
 *
 * Defines a contract for configuring a {@link SecurityBuilder}.
 * <br>
 * Works in two phases:
 * <ul>
 *   <li>🔧 {@link #initialize(SecurityBuilder)} – one-time setup before building</li>
 *   <li>🛠️ {@link #configure(SecurityBuilder)} – fine-tune configuration</li>
 * </ul>
 *
 * @param <T> the type built by {@link SecurityBuilder}
 * @param <B> the concrete builder type
 */
public interface SecurityConfigurer<T, B extends SecurityBuilder<T>> {

    /**
     * 🔧 Initialization step.
     * <br>
     * Called once before the actual {@link #configure(SecurityBuilder)} phase.
     * Use this for setting defaults or preparing shared state.
     *
     * @param builder the security builder
     * @throws Exception in case of setup errors
     */
    default void initialize(B builder) throws Exception {
    }

    /**
     * 🛠️ Configuration step.
     * <br>
     * Invoked after {@link #initialize(SecurityBuilder)} to apply detailed settings,
     * add filters, or customize policies.
     *
     * @param builder the security builder
     * @throws Exception in case of configuration errors
     */
    default void configure(B builder) throws Exception {
    }

}
