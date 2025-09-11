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
     * 🔗 Underlying delegate chain of resolvers.
     */
    private final Chain<HttpServletRequest, ResourceQuery, Resource> delegate;

    /**
     * 🏗️ Create a new resolver chain.
     *
     * @param resolvers ordered list of {@link ResourceResolver}s to apply
     */
    public SimpleResourceResolverChain(List<? extends ResourceResolver> resolvers) {
        this.delegate = Chain.of(resolvers);
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
     * 📝 Debug-friendly string representation of this chain.
     *
     * @return human-readable chain description
     */
    @Override
    public String toString() {
        return "Chain[delegate=%s]".formatted(delegate);
    }
}
