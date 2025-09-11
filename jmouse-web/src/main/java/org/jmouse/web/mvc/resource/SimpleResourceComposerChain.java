package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;

import java.util.List;

public class SimpleResourceComposerChain implements ResourceComposerChain {

    private final Chain<HttpServletRequest, ResourceQuery, String> delegate;

    public SimpleResourceComposerChain(List<? extends ResourceComposer> composers) {
        this.delegate = Chain.of(composers);
    }

    @Override
    public Outcome<String> proceed(HttpServletRequest request, ResourceQuery resourceQuery) {
        return delegate.proceed(request, resourceQuery);
    }

    @Override
    public String toString() {
        return "Chain[delegate=%s]".formatted(delegate);
    }
}
