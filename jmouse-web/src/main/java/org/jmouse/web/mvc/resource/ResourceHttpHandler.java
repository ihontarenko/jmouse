package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.core.MediaTypeFactory;
import org.jmouse.core.Streamable;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.request.CacheControl;
import org.jmouse.web.mvc.HandlerMapping;
import org.jmouse.web.mvc.RouteMatch;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ResourceHttpHandler implements RequestHttpHandler {

    private final ResourceRegistration         registration;
    private final PatternMatcherResourceLoader loader;
    private final MediaTypeFactory             factory;

    public ResourceHttpHandler(ResourceRegistration registration, PatternMatcherResourceLoader loader, MediaTypeFactory factory) {
        this.registration = registration;
        this.loader = loader;
        this.factory = factory;
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
        ResourceResolverChain chain    = getResolverChain(registration.getChainRegistration().getResolvers());
        ResourceQuery         query    = getResourceQuery(request, registration.getLocations());
        Resource              resource = chain.resolve(request, query);

        if (resource != null && resource.isReadable()) {
            writeHeaders(response, resource);

            InputStream  input  = resource.getInputStream();
            OutputStream output = response.getOutputStream();

            input.transferTo(output);
        }
    }

    private void writeHeaders(HttpServletResponse response, Resource resource) {
        MediaType mediaType = factory.getMediaType(resource.getName());

        response.setContentType(mediaType.toString());

        if (resource instanceof HttpResource httpResource) {
            httpResource.getHeaders().asMap().forEach(
                    (httpHeader, value) -> response.setHeader(httpHeader.value(), String.valueOf(value)));
        }

        response.setHeader(HttpHeader.ACCEPT_RANGES.value(), "bytes");

        CacheControl cacheControl = registration.getCacheControl();
        Integer      seconds      = registration.getCachePeriod();

        if (seconds != null) {
            cacheControl = CacheControl.empty()
                    .maxAge(registration.getCachePeriod(), TimeUnit.SECONDS).mustRevalidate();
            response.setDateHeader(HttpHeader.EXPIRES.value(), System.currentTimeMillis() + seconds * 1000L);
        }

        if (cacheControl != null) {
            response.setHeader(HttpHeader.CACHE_CONTROL.value(), cacheControl.toHeaderValue());
        }

        response.setHeader(HttpHeader.CONTENT_LENGTH.value(), String.valueOf(resource.getSize()));
    }

    private String getResourcePath(HttpServletRequest request) {
        String resourcePath = "";

        if (request.getAttribute(HandlerMapping.ROUTE_MATCH_ATTRIBUTE) instanceof RouteMatch match) {
            resourcePath = match.extracted();
        }

        return resourcePath;
    }

    private List<Resource> getResourceLocations(List<String> locations) {
        List<Resource> resources = List.of();

        if (locations != null && !locations.isEmpty()) {
            resources = Streamable.of(registration.getLocations()).map(loader::getResource).toList();
        }

        return resources;
    }

    private ResourceResolverChain getResolverChain(List<ResourceResolver> resolvers) {
        return new SimpleResourceResolverChain(registration.getChainRegistration().getResolvers());
    }

    private ResourceQuery getResourceQuery(HttpServletRequest request, List<String> locations) {
        return new ResourceQuery(getResourcePath(request), getResourceLocations(locations));
    }

}
