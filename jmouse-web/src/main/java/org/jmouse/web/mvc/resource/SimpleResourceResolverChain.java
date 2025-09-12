package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;

import java.util.List;

/**
 * ⛓️ Default implementation of {@link ResourceResolverChain}.
 *
 * <p>Builds a resolver chain from a list of {@link ResourceResolver}s,
 * delegating resolution step by step until a match is found.</p>
 */
public class SimpleResourceResolverChain implements ResourceResolverChain {

    /**
     * 🔗 Chain of {@link ResourceResolver}s for resource resolution.
     *
     * <p>Executes each resolver in order until one produces a
     * {@link org.jmouse.core.chain.Outcome.Done} with a {@link Resource}.</p>
     */
    private final Chain<HttpServletRequest, ResourceQuery, Resource> delegate;

    /**
     * 🎼 Chain of {@link ResourceComposer}s for resource composition.
     *
     * <p>Transforms resolved resources (e.g. add version, rewrite URL)
     * into a final {@link String} path.</p>
     */
    private final Chain<String, UrlComposerContext, String> composer;

    /**
     * 🏗️ Create a new resolver chain.
     *
     * <p>Builds two parallel chains:</p>
     * <ul>
     *   <li>🔗 {@code delegate} — executes the {@link ResourceResolver}s step by step</li>
     *   <li>🎼 {@code composer} — applies each resolver’s {@link ResourceComposer}</li>
     * </ul>
     *
     * @param resolvers ordered list of {@link ResourceResolver}s to apply
     */
    public SimpleResourceResolverChain(List<? extends ResourceResolver> resolvers) {
        this.delegate = Chain.of(resolvers, false);
        this.composer = Chain.of(resolvers.stream().map(ResourceResolver::getComposer).toList(), true);
    }

    /**
     * ▶️ Continue resolution with the next {@link ResourceResolver}.
     *
     * @param request       incoming HTTP request
     * @param resourceQuery query describing the requested resource
     * @return {@link Outcome} of resolved {@link Resource}, or empty if none matched
     */
    @Override
    public Outcome<Resource> proceed(HttpServletRequest request, ResourceQuery resourceQuery) {
        return delegate.proceed(request, resourceQuery);
    }

    /**
     * 🎼 Apply the composition chain to a resolved {@link Resource}.
     *
     * <p>Each resolver’s {@link ResourceComposer} may transform the
     * resource path (e.g. add versioning, rewrite URL, etc.).</p>
     *
     * @param relative      the relative path to compose final resource URL
     * @param context       the query context context
     * @return the composed resource path, or {@code null} if no composer produced a value
     */
    @Override
    public String compose(String relative, UrlComposerContext context) {
        Outcome<String> outcome = composer.proceed(relative, context);

        if (outcome instanceof Outcome.Done<String>(String value)) {
            return value;
        }

        return null;
    }

    /**
     * 📝 Debug-friendly string representation of this chain.
     *
     * @return human-readable chain description
     */
    @Override
    public String toString() {
        return "Chain[delegate=%s]".formatted(delegate);
    }
}
