package org.jmouse.security.web.configuration;

public abstract class AbstractSecurityBuilder<T> implements SecurityBuilder<T> {

    private boolean built;

    @Override
    public final T build() throws Exception {
        if (!built) {
            built = true;
            return doBuild();
        }
        throw new IllegalStateException("This object has already been built");
    }

    protected abstract T doBuild() throws Exception;
}
