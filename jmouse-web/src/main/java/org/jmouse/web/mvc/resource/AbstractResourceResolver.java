package org.jmouse.web.mvc.resource;

/**
 * 🏗️ Base class for {@link ResourceResolver} implementations.
 *
 * <p>Provides common support for attaching a {@link ResourceComposer}
 * used to post-process resolved resources (e.g. appending versions,
 * transforming URLs, etc.).</p>
 *
 * <p>💡 Extend this class when implementing custom resolvers
 * instead of re-implementing composer handling logic.</p>
 */
public abstract class AbstractResourceResolver implements ResourceResolver {

    /**
     * 🎼 Composer used for resource post-processing.
     */
    protected ResourceComposer composer;

    /**
     * ⚙️ Create a new resolver with the given {@link ResourceComposer}.
     *
     * @param composer resource composer to apply after resolution
     */
    protected AbstractResourceResolver(ResourceComposer composer) {
        this.composer = composer;
    }

    /**
     * 🎼 Return the configured {@link ResourceComposer}.
     *
     * @return the composer, never {@code null}
     */
    @Override
    public ResourceComposer getComposer() {
        return composer;
    }

    /**
     * 🔧 Update the {@link ResourceComposer} for this resolver.
     *
     * <p>Allows dynamic replacement of the composer strategy,
     * useful for testing or reconfiguration.</p>
     *
     * @param composer new composer to assign (must not be {@code null})
     */
    public void setComposer(ResourceComposer composer) {
        this.composer = composer;
    }
}
