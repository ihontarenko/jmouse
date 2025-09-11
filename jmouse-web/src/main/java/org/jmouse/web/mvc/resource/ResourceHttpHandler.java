package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.Streamable;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.RequestPath;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;

import java.io.IOException;
import java.util.List;

public class ResourceHttpHandler implements RequestHttpHandler {

    private final ResourceRegistration         registration;
    private   final     PatternMatcherResourceLoader loader;

    public ResourceHttpHandler(ResourceRegistration registration, PatternMatcherResourceLoader loader) {
        this.registration = registration;
        this.loader = loader;
    }

    /**
     * Handles the incoming request and writes the response.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @throws IOException in case of I/O errors
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RequestPath           requestPath = RequestAttributesHolder.getRequestPath();
        ResourceResolverChain chain       = new SimpleResourceResolverChain(
                registration.getChainRegistration().getResolvers());
        List<Resource>        locations   = Streamable.of(registration.getLocations()).map(loader::getResource)
                .toList();
        Resource              resource    = chain.resolve(request, new ResourceQuery(requestPath.path(), locations));

        if (resource != null && resource.isReadable()) {
            System.out.println(resource);
        }

        System.out.println(resource);
    }

}
