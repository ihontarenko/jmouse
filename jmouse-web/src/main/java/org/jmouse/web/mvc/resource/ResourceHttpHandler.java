package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.core.MediaTypeFactory;
import org.jmouse.core.Streamable;
import org.jmouse.core.io.PatternMatcherResourceLoader;
import org.jmouse.core.io.Resource;
import org.jmouse.core.io.ResourceSegment;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.request.CacheControl;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.Range;
import org.jmouse.web.http.request.WebHttpRequest;
import org.jmouse.web.mvc.*;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;
import org.jmouse.web.mvc.method.converter.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 🗂️ HTTP handler for serving static {@link Resource}s.
 *
 * <p>Resolution pipeline:</p>
 * <ol>
 *   <li>Build a {@link ResourceResolverChain} from the registration</li>
 *   <li>Create a {@link ResourceQuery} (extracted path + locations)</li>
 *   <li>Resolve a {@link Resource} via the chain</li>
 *   <li>Write HTTP headers and stream the resource via a matching {@link HttpMessageConverter}</li>
 * </ol>
 *
 * <p>Caching:</p>
 * <ul>
 *   <li>Supports both fixed {@code cachePeriod} and custom {@link CacheControl}</li>
 *   <li>Adds {@code Accept-Ranges: bytes} and {@code Content-Length}</li>
 * </ul>
 */
public class ResourceHttpHandler implements RequestHttpHandler {

    /**
     * 📋 Registration containing patterns, locations, chain and cache settings.
     */
    private final ResourceRegistration         registration;
    /**
     * 🔎 Loader to resolve location strings into {@link Resource}s (classpath/file, etc.).
     */
    private final PatternMatcherResourceLoader resourceLoader;
    /**
     * 🔁 Finds an {@link HttpMessageConverter} that can write the resolved resource.
     */
    private final MessageConverterManager      messageConverterManager;
    /**
     * 🏭 Determines response {@link MediaType} based on resource name/extension.
     */
    private final MediaTypeFactory             mediaTypeFactory;

    /**
     * 🏗️ Create a new resource HTTP handler.
     *
     * @param registration            resource handler registration
     * @param resourceLoader          loader for resolving base locations
     * @param messageConverterManager manager for HTTP message converters
     * @param mediaTypeFactory        factory for media type lookups
     */
    public ResourceHttpHandler(
            ResourceRegistration registration,
            PatternMatcherResourceLoader resourceLoader,
            MessageConverterManager messageConverterManager,
            MediaTypeFactory mediaTypeFactory
    ) {
        this.registration = registration;
        this.resourceLoader = resourceLoader;
        this.messageConverterManager = messageConverterManager;
        this.mediaTypeFactory = mediaTypeFactory;
    }

    /**
     * 📂 Get the associated {@link ResourceRegistration}.
     *
     * <p>Provides access to handler-specific configuration such as
     * resource locations, cache settings, and URL patterns.</p>
     *
     * @return the resource handler registration metadata
     */
    public ResourceRegistration getRegistration() {
        return registration;
    }

    /**
     * 🚚 Handle the incoming request and write the static resource response.
     *
     * <p>Resolves a resource from configured locations via the resolver chain.
     * If found and readable, writes headers and streams the content.</p>
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @throws IOException in case of I/O errors while writing the response
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ResourceResolverChain chain    = getResolverChain(registration.getChainRegistration().getResolvers());
        ResourceQuery         query    = getResourceQuery(request, registration.getLocations());
        Resource              resource = chain.resolve(request, query);
        List<Range>           ranges   = null;

        if (request instanceof WebHttpRequest webHttpRequest) {
            ranges = webHttpRequest.getHeaders().getRange();
        }

        if (resource != null && resource.isReadable()) {
            HttpOutputMessage httpMessage = new ServletResponseHttpOutputMessage(response);
            writeHeaders(httpMessage, resource);
            writeMessage(httpMessage, resource, ranges);
        }
    }

    /**
     * 📤 Write the resolved {@link Resource} body using a suitable converter.
     *
     * <p>Chooses a {@link HttpMessageConverter} via {@link MessageConverterManager}
     * and delegates the actual streaming. Errors during write are wrapped into
     * {@link UnwritableException}.</p>
     *
     * @param httpMessage HTTP output message
     * @param resource resolved resource to write
     */
    private void writeMessage(HttpOutputMessage httpMessage, Resource resource, List<Range> ranges) {
        Object                       writable         = resource;
        HttpMessageConverter<Object> messageConverter = messageConverterManager.getMessageConverter(writable, null);

        if (ranges != null && !ranges.isEmpty()) {
            List<ResourceSegment> segments = getResourceSegments(ranges, resource);

            if (segments != null && !segments.isEmpty()) {
                messageConverter = messageConverterManager
                        .getMessageConverter(segments.getFirst(), null);

                if (messageConverter != null) {
                    writable = segments;
                }
            }
        }

        if (messageConverter != null) {
            try {
                messageConverter.write(writable, writable.getClass(), httpMessage);
            } catch (IOException e) {
                throw new UnwritableException("Could not write HTTP message", e);
            }
        }
    }

    /**
     * 🧾 Compute and set HTTP headers for the response.
     *
     * <ul>
     *   <li>Sets {@code Content-Type} from {@link MediaTypeFactory}</li>
     *   <li>Copies headers from {@link HttpResource}, if applicable</li>
     *   <li>Sets {@code Accept-Ranges: bytes}</li>
     *   <li>Applies {@link CacheControl} or {@code cachePeriod} (also sets {@code Expires})</li>
     *   <li>Sets {@code Content-Length} from resource size</li>
     * </ul>
     *
     * @param httpMessage HTTP output message
     * @param resource resolved resource
     */
    private void writeHeaders(HttpOutputMessage httpMessage, Resource resource) {
        Headers   headers   = httpMessage.getHeaders();
        MediaType mediaType = mediaTypeFactory.getMediaType(resource.getName());

        headers.setContentType(mediaType);

        if (resource instanceof HttpResource httpResource) {
            httpResource.getHeaders().asMap().forEach(
                    (httpHeader, value) -> headers.setHeader(httpHeader, String.valueOf(value)));
        }

        headers.setHeader(HttpHeader.ACCEPT_RANGES, "bytes");

        CacheControl cacheControl = registration.getCacheControl();
        Integer      seconds      = registration.getCachePeriod();

        if (seconds != null) {
            cacheControl = CacheControl.empty()
                    .maxAge(registration.getCachePeriod(), TimeUnit.SECONDS).mustRevalidate();
            headers.setHeader(HttpHeader.EXPIRES, System.currentTimeMillis() + seconds * 1000L);
        }

        if (cacheControl != null) {
            headers.setHeader(HttpHeader.CACHE_CONTROL, cacheControl.toHeaderValue());
        }

        if (headers.getRange() == null) {
            headers.setHeader(HttpHeader.CONTENT_LENGTH, String.valueOf(resource.getSize()));
        }
    }

    private List<ResourceSegment> getResourceSegments(List<Range> ranges, Resource resource) {
        List<ResourceSegment> segments = new ArrayList<>();

        if (ranges != null) {
            segments = ResourceRange.toSegments(ranges, resource);
        }

        return segments;
    }

    /**
     * 🗺️ Extract the resource path from the current route match.
     *
     * <p>Uses {@link HandlerMapping#ROUTE_MATCH_ATTRIBUTE} to obtain the portion
     * of the request path that should be resolved against configured locations.</p>
     *
     * @param request current request
     * @return extracted resource path or empty string if not available
     */
    private String getResourcePath(HttpServletRequest request) {
        String resourcePath = "";
        String queryString  = request.getQueryString();

        if (request.getAttribute(HandlerMapping.ROUTE_MATCH_ATTRIBUTE) instanceof RouteMatch match) {
            resourcePath = match.extracted();
        }

        if (!resourcePath.isEmpty() && queryString != null && !queryString.isEmpty()) {
            resourcePath += "?" + queryString;
        }

        return resourcePath;
    }

    /**
     * 📍 Convert registration locations into concrete {@link Resource} bases.
     *
     * @param locations list of location strings (classpath/file/etc.)
     * @return resolved resource bases (may be empty)
     */
    public List<Resource> getResourceLocations(List<String> locations) {
        List<Resource> resources = List.of();

        if (locations != null && !locations.isEmpty()) {
            resources = Streamable.of(registration.getLocations()).map(resourceLoader::getResource).toList();
        }

        return resources;
    }

    /**
     * ⛓️ Build a {@link ResourceResolverChain} from registered resolvers.
     *
     * @param resolvers configured resolvers in order
     * @return chain to use for resolution
     */
    public ResourceResolverChain getResolverChain(List<ResourceResolver> resolvers) {
        return new SimpleResourceResolverChain(resolvers);
    }

    /**
     * 📨 Create a {@link ResourceQuery} for the current request.
     *
     * @param relativePath current relative path
     * @param locations configured base locations
     * @return query holding extracted path and resolved base resources
     */
    public ResourceQuery getResourceQuery(String relativePath, List<String> locations) {
        return new ResourceQuery(relativePath, getResourceLocations(locations));
    }

    /**
     * 📨 Create a {@link ResourceQuery} for the current request.
     *
     * @param request   current request
     * @param locations configured base locations
     * @return query holding extracted path and resolved base resources
     */
    public ResourceQuery getResourceQuery(HttpServletRequest request, List<String> locations) {
        return getResourceQuery(getResourcePath(request), locations);
    }
}
