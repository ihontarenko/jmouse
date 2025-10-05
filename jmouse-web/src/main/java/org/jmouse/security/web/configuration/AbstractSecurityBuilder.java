package org.jmouse.security.web.configuration;

public abstract class AbstractSecurityBuilder<T> implements SecurityBuilder<T> {

    private boolean built = false;

    @Override
    public final T build() throws Exception {
        if (!built) {
            built = true;
            return doBuild();
        }
        throw new IllegalStateException(
                "Security builder '%s' has already been built.".formatted(
                        this.getClass().getSimpleName()));
    }

    protected abstract T doBuild() throws Exception;
}
