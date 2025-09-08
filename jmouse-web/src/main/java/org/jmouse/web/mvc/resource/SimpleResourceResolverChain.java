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

    private final Chain<HttpServletRequest, ResourceQuery, Resource> delegate;

    public SimpleResourceResolverChain(List<? extends ResourceResolver> resolvers) {
        this.delegate = Chain.of(resolvers);
    }

    @Override
    public Outcome<Resource> proceed(HttpServletRequest request, ResourceQuery resourceQuery) {
        return delegate.proceed(request, resourceQuery);
    }

    @Override
    public String toString() {
        return "Chain[delegate=%s]".formatted(delegate);
    }
}
