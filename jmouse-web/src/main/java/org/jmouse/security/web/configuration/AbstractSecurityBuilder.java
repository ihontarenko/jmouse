package org.jmouse.security.web.configuration;

/**
 * 🏗️ {@code AbstractSecurityBuilder}
 *
 * Base implementation of {@link SecurityBuilder} with lifecycle control.
 * <br>
 * Ensures a builder is used only once.
 *
 * <ul>
 *   <li>✅ {@link #build()} – final entry point, enforces single-use</li>
 *   <li>⚙️ {@link #doBuild()} – subclass hook to construct the target</li>
 *   <li>🚫 Throws {@link IllegalStateException} if reused</li>
 * </ul>
 *
 * @param <T> type of object being built
 */
public abstract class AbstractSecurityBuilder<T> implements SecurityBuilder<T> {

    /** 🔒 Tracks if the builder has already produced an instance */
    private boolean built = false;

    /**
     * ✅ Final build method.
     * <br>
     * - Executes {@link #doBuild()} once.
     * - Marks builder as used.
     * - Fails fast if called again.
     *
     * @return the constructed object
     * @throws Exception if building fails
     * @throws IllegalStateException if called more than once
     */
    @Override
    public final T build() throws Exception {
        if (!built) {
            built = true;
            return doBuild();
        }
        throw new IllegalStateException(
                "Security builder '%s' has already been built."
                        .formatted(this.getClass().getSimpleName()));
    }

    /**
     * ⚙️ Template method for subclasses.
     * <br>
     * Defines how the target object is created.
     *
     * @return the constructed object
     * @throws Exception if creation fails
     */
    protected abstract T doBuild() throws Exception;
}
