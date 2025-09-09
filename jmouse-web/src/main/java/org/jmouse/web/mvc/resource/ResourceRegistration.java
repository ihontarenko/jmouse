package org.jmouse.web.mvc.resource;

import org.jmouse.web.http.request.CacheControl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 📂 Defines a resource handler registration.
 *
 * <p>Holds configuration for mapping resource patterns to
 * locations and customizing caching behavior.</p>
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>🛣️ URL patterns (e.g. {@code /static/**}, {@code /assets/{path:\\w\\.[css|js]}})</li>
 *   <li>📍 Resource locations (classpath or file system)</li>
 *   <li>⛓️ Resource chain customization</li>
 *   <li>🗄️ Cache period or full {@link CacheControl}</li>
 *   <li>⏱️ Use of {@code Last-Modified} header</li>
 * </ul>
 */
public class ResourceRegistration {

    /**
     * 📍 Resource locations to serve from.
     */
    private final List<String> locations = new ArrayList<>();

    /**
     * 🛣️ Ant-style patterns handled by this registration.
     */
    private final List<String> patterns = new ArrayList<>();

    /**
     * ⛓️ Chain configuration for resolvers/transformers.
     */
    private final ResourceChainRegistration chainRegistration = new ResourceChainRegistration();

    /**
     * ⏳ Cache period in seconds (optional).
     */
    private Integer cachePeriod;

    /**
     * 🗄️ Cache control policy (optional).
     */
    private CacheControl cacheControl;

    /**
     * ⏱️ Whether to send {@code Last-Modified} header.
     */
    private boolean useLastModified = true;

    /**
     * ➕ Add URL patterns to this registration.
     *
     * @param patterns ant-style patterns
     * @return this registration for chaining
     */
    public ResourceRegistration addPatterns(String... patterns) {
        this.patterns.addAll(Arrays.asList(patterns));
        return this;
    }

    /**
     * ➕ Add resource locations to this registration.
     *
     * @param locations resource base locations
     * @return this registration for chaining
     */
    public ResourceRegistration addResourceLocations(String... locations) {
        this.locations.addAll(Arrays.asList(locations));
        return this;
    }

    /**
     * 📍 Get registered resource locations.
     */
    public List<String> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    /**
     * ⛓️ Get chain registration for resolvers and transformers.
     */
    public ResourceChainRegistration getChainRegistration() {
        return chainRegistration;
    }

    /**
     * 🛣️ Get registered URL patterns.
     */
    public List<String> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }

    /**
     * ⏳ Set cache period in seconds.
     *
     * @param cachePeriod time-to-live in seconds
     * @return this registration
     */
    public ResourceRegistration setCachePeriod(Integer cachePeriod) {
        this.cachePeriod = cachePeriod;
        return this;
    }

    /**
     * 🗄️ Set full cache control policy.
     *
     * @param cacheControl configured cache control
     * @return this registration
     */
    public ResourceRegistration setCacheControl(CacheControl cacheControl) {
        this.cacheControl = cacheControl;
        return this;
    }

    /**
     * ⏱️ Enable or disable {@code Last-Modified} header support.
     *
     * @param useLastModified whether to send {@code Last-Modified}
     * @return this registration
     */
    public ResourceRegistration setUseLastModified(boolean useLastModified) {
        this.useLastModified = useLastModified;
        return this;
    }

    @Override
    public String toString() {
        return "RESOURCE_REGISTRATION %s: %s".formatted(patterns, locations);
    }
}
