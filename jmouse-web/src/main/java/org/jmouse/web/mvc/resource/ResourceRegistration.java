package org.jmouse.web.mvc.resource;

import org.jmouse.web.http.Allow;
import org.jmouse.web.http.CacheControl;
import org.jmouse.web.http.ETag;
import org.jmouse.web.http.Vary;
import org.jmouse.web.mvc.ETagGenerator;

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
     * Controls emission of the {@code Last-Modified} header when a valid timestamp is available.
     * <p>Default: {@code true}. Typically relevant for safe methods (GET/HEAD) and 200/304 flows.</p>
     */
    private boolean useLastModified = true;

    /**
     * Strategy used to compute entity tags (ETag).
     *
     * <p>If {@code null}, ETag generation is disabled. Provide a custom implementation
     * for strong ETags (e.g., hashing the actual representation bytes) or rely on
     * the default length/mtime-based approach for weak ETags.</p>
     *
     * @see ETag
     */
    private ETagGenerator etagGenerator;

    /**
     * Preconfigured {@code Vary} policy applied by {@link ResourceHttpHandler#prepareResponse()}.
     * <p>Use {@link Vary#empty()} to suppress emission; {@code null} means “no configured policy”.</p>
     *
     * @see org.jmouse.web.http.HttpHeader#VARY
     */
    private Vary vary = Vary.empty();

    /**
     * Model used to render the {@code Allow} header (e.g., for {@code OPTIONS} responses).
     * <p>Usually computed from the supported methods; use {@link Allow#empty()} to emit nothing.</p>
     *
     * @see org.jmouse.web.http.HttpHeader#ALLOW
     */
    private Allow allow = Allow.empty();

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

    /**
     * 📆 Return configured cache period in seconds (if any).
     *
     * @return cache period, or {@code null} if not set
     */
    public Integer getCachePeriod() {
        return cachePeriod;
    }

    /**
     * 📋 Return configured {@link CacheControl} directives (if any).
     *
     * @return cache control, or {@code null} if not set
     */
    public CacheControl getCacheControl() {
        return cacheControl;
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
     * Returns the configured {@link Vary} header model.
     *
     * @return current {@code Vary} configuration, or {@code null} if none
     * @see org.jmouse.web.http.HttpHeader#VARY
     */
    public Vary getVary() {
        return vary;
    }

    /**
     * Sets the {@link Vary} header values to apply.
     * <p>Passing {@code null} clears the configuration. If the value is empty,
     * the {@code Vary} header will not be emitted.</p>
     *
     * @param vary the {@code Vary} configuration to use, or {@code null} to clear
     * @return this registration
     * @see org.jmouse.web.http.HttpHeader#VARY
     */
    public ResourceRegistration setVary(Vary vary) {
        this.vary = vary;
        return this;
    }

    /**
     * Returns the {@link Allow} model used to render the {@code Allow} header.
     *
     * @return current {@code Allow} configuration; may be {@code null}
     * @see org.jmouse.web.http.HttpHeader#ALLOW
     */
    public Allow getAllow() {
        return allow;
    }

    /**
     * Sets the {@link Allow} model used to render the {@code Allow} header.
     * <p>Callers are responsible for providing a value consistent with the
     * actually supported methods (e.g., including {@code OPTIONS} when applicable).</p>
     *
     * @param allow the {@code Allow} configuration to use; may be {@code null}
     * @return this registration
     * @see org.jmouse.web.http.HttpHeader#ALLOW
     */
    public ResourceRegistration setAllow(Allow allow) {
        this.allow = allow;
        return this;
    }

    /**
     * Returns the configured ETag generator strategy, if any.
     *
     * @return the {@link ETagGenerator} in use, or {@code null} if ETag generation is disabled
     * @see ETag
     */
    public ETagGenerator getEtagGenerator() {
        return etagGenerator;
    }

    /**
     * Sets the ETag generator strategy.
     * <p>Pass {@code null} to disable ETag generation.</p>
     *
     * @param etagGenerator the {@link ETagGenerator} to use, or {@code null} to disable
     * @return this registration
     * @see ETag
     */
    public ResourceRegistration setEtagGenerator(ETagGenerator etagGenerator) {
        this.etagGenerator = etagGenerator;
        return this;
    }

    @Override
    public String toString() {
        return "RESOURCE_REGISTRATION %s: %s".formatted(patterns, locations);
    }
}
