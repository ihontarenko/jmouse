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
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.request.*;
import org.jmouse.web.mvc.*;
import org.jmouse.web.mvc.adapter.RequestHttpHandler;
import org.jmouse.web.mvc.method.converter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
public class ResourceHttpHandler extends WebResponder implements RequestHttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceHttpHandler.class);

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
     * Controls emission of the {@code Last-Modified} response header when a valid timestamp is available.
     * <p>Default: {@code true}.</p>
     */
    private boolean useLastModified = false;

    /**
     * Strategy used to compute entity tags (ETag).
     *
     * <p>If {@code null}, ETag generation is disabled. Provide a custom implementation
     * for strong ETags (e.g., hashing the actual representation bytes) or rely on
     * the default length/mtime-based approach for weak ETags.</p>
     *
     * @see org.jmouse.web.http.request.ETag
     */
    private ETagGenerator generator;

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
        ResourceResolverChain chain           = getResolverChain(registration.getChainRegistration().getResolvers());
        ResourceQuery         query           = getResourceQuery(request, registration.getLocations());
        Resource              resource        = chain.resolve(request, query);
        Headers               requestHeaders  = RequestAttributesHolder.getRequestHeaders().headers();
        HttpMethod            httpMethod      = requestHeaders.getMethod();
        Headers               responseHeaders = getHeaders();

        if (resource == null || !resource.isReadable()) {
            throw new ResourceNotFoundException(
                    "RESOURCE NOT FOUND! URI: " + RequestAttributesHolder.getRequestPath().path());
        }

        if (maybeHandleOptions(httpMethod, response)) {
            return;
        }

        checkRequest(httpMethod, false);

        ETag                etag            = getGeneratedETag(resource);
        HttpExchangeSupport exchangeSupport = new HttpExchangeSupport(requestHeaders, responseHeaders);
        long                modified        = isUseLastModified() ? resource.getLastModified() : -1;
        boolean             notModified     = exchangeSupport.checkNotModified(etag, modified);

        prepareResponse();

        writeHeaders(response);

        if (notModified) {
            return;
        }

        HttpOutputMessage httpMessage    = new ServletHttpOutputMessage(response);
        Headers           messageHeaders = httpMessage.getHeaders();

        writeHeaders(messageHeaders, resource);
        writeMessage(httpMessage, resource, request);
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
    private void writeMessage(HttpOutputMessage httpMessage, Resource resource, HttpServletRequest request) {
        Object                       writable         = resource;
        List<Range>                  ranges           = null;
        HttpMessageConverter<Object> messageConverter = messageConverterManager
                .getMessageConverter(writable, null);

        if (request instanceof WebHttpRequest webHttpRequest) {
            ranges = webHttpRequest.getHeaders().getRange();
        }

        if (ranges != null && !ranges.isEmpty()) {
            List<ResourceSegment> segments = getResourceSegments(ranges, resource);

            if (segments != null && !segments.isEmpty()) {
                messageConverter = messageConverterManager
                        .getMessageConverter(segments.getFirst(), null);
                if (messageConverter != null) {
                    writable = segments;
                }
            }
        } else if (request instanceof WebRequest webRequest && webRequest.getHttpMethod() == HttpMethod.HEAD) {
            if (messageConverter instanceof AbstractHttpMessageConverter<Object> httpMessageConverter) {
                MediaType mediaType = mediaTypeFactory.getMediaType(resource.getName());
                httpMessageConverter.writeDefaultHeaders(httpMessage, writable, mediaType);
                if (httpMessage instanceof ServletHttpOutputMessage servletHttpOutputMessage) {
                    servletHttpOutputMessage.writeHeaders();
                }
            }
            return;
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
     * @param headers buffered output headers
     * @param resource resolved resource
     */
    private void writeHeaders(Headers headers, Resource resource) {
        MediaType mediaType = mediaTypeFactory.getMediaType(resource.getName());

        headers.setContentType(mediaType);

        if (resource instanceof HttpResource httpResource) {
            httpResource.getHeaders().asMap().forEach(
                    (httpHeader, value) -> headers.setHeader(httpHeader, String.valueOf(value)));
        }

        headers.setHeader(HttpHeader.ACCEPT_RANGES, "bytes");

        if (headers.getRange() == null) {
            headers.setHeader(HttpHeader.CONTENT_LENGTH, String.valueOf(resource.getLength()));
        }
    }

    /**
     * Resolves HTTP byte ranges into concrete {@link ResourceSegment}s.
     *
     * <p>Returns an empty list when {@code ranges} is {@code null} or empty. Never returns {@code null}.
     * Callers may treat an empty list as “no range requested” (serve full body).</p>
     *
     * @param ranges   parsed byte ranges from the request; may be {@code null}
     * @param resource target resource used to clamp/validate ranges
     * @return list of resolved segments (possibly empty)
     */
    private List<ResourceSegment> getResourceSegments(List<Range> ranges, Resource resource) {
        List<ResourceSegment> segments = new ArrayList<>();

        if (ranges != null) {
            segments = ResourceRange.toSegments(ranges, resource);
        }

        return segments;
    }

    /**
     * Computes an {@link ETag} for the given resource using the configured {@link ETagGenerator}.
     *
     * <p>Returns {@code null} if no generator is configured. The seed format is
     * {@code "length:lastModified"} (see {@link ETagGenerator#seed(long, long)}).</p>
     *
     * @param resource the resource to describe
     * @return generated ETag or {@code null} if generation is disabled
     */
    private ETag getGeneratedETag(Resource resource) {
        String seed = resource.getLength() + ":" + resource.getLastModified();
        return generator == null ? null : generator.generate(seed);
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

    /**
     * ⏱️ Enable or disable {@code Last-Modified} header support.
     *
     * @param useLastModified whether to send {@code Last-Modified}
     */
    public void setUseLastModified(boolean useLastModified) {
        this.useLastModified = useLastModified;
    }

    /**
     * ⏱️ Whether {@code Last-Modified} header support is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean isUseLastModified() {
        return useLastModified;
    }

    /**
     * Returns the configured ETag generator strategy, if any.
     *
     * @return the {@link ETagGenerator} in use, or {@code null} if ETag generation is disabled
     * @see org.jmouse.web.http.request.ETag
     */
    public ETagGenerator getGenerator() {
        return generator;
    }

    /**
     * Sets the ETag generator strategy.
     * <p>Pass {@code null} to disable ETag generation.</p>
     *
     * @param generator the {@link ETagGenerator} to use, or {@code null} to disable
     * @see org.jmouse.web.http.request.ETag
     */
    public void setGenerator(ETagGenerator generator) {
        this.generator = generator;
    }

}
