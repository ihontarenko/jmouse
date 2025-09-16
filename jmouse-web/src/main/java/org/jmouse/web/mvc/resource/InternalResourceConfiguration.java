package org.jmouse.web.mvc.resource;

import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.request.Allow;
import org.jmouse.web.http.request.Vary;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Default internal configuration for serving static resources.
 *
 * <p>Registers a {@code /public/{*path}} handler that resolves from
 * {@code classpath:public/}, applies a resolver chain, and configures
 * HTTP semantics (Allow/Vary).</p>
 *
 * <p>Intended to be auto-detected as a bean.</p>
 *
 * @see ResourceHandlerRegistry
 * @see ResourceRegistration
 * @see VersionalResourceResolver
 * @see WebBeanContext
 */
@Bean
public class InternalResourceConfiguration implements InitializingBeanSupport<WebBeanContext> {

    private static final DateTimeFormatter YYYY_MM =
            DateTimeFormatter.ofPattern("yyyy.MM").withZone(ZoneId.systemDefault());

    private static String currentYearMonthVersion() {
        // e.g. "v2025.09"
        return "v" + YYYY_MM.format(Instant.now());
    }

    /**
     * Registry collecting default resource handler registrations.
     */
    private final ResourceHandlerRegistry registry = new ResourceHandlerRegistry();

    /**
     * Initializes default resource handling:
     * <ul>
     *   <li>Maps {@code /public/{*path}} to {@code classpath:public/}.</li>
     *   <li>Restricts methods to {@code GET, HEAD} (emits {@code Allow}).</li>
     *   <li>Sets {@code Vary: Accept-Language, Accept-Charset}.</li>
     *   <li>Applies resolver chain:
     *     <ol>
     *       <li>{@link PathNormalizationResolver}</li>
     *       <li>{@link VersionalResourceResolver} (fixed or content-hash)</li>
     *       <li>{@link LocationScanningResolver}</li>
     *     </ol>
     *   </li>
     * </ul>
     *
     * @param context active web bean context
     */
    @Override
    public void doInitialize(WebBeanContext context) {
        registry.registerHandler("/public/{*path}")
                .setAllow(Allow.of(HttpMethod.GET, HttpMethod.HEAD))
                .setVary(Vary.of(HttpHeader.ACCEPT_LANGUAGE, HttpHeader.ACCEPT_CHARSET))
                .addPatterns("classpath:public/")
                .getChainRegistration()
                .addResolvers(
                        new PathNormalizationResolver(),
                        getVersionalResourceResolver(),
                        new LocationScanningResolver()
                );
    }

    /**
     * Exposes the default resource handler registrations created by this config.
     *
     * @return immutable list of registrations
     * @see ResourceRegistration
     */
    public List<ResourceRegistration> getDefaultRegistrations() {
        return registry.getRegistrations();
    }

    /**
     * Builds the versioning resolver used in the chain:
     * <ul>
     *   <li><b>Fixed version</b> {@code v2025.09} for {@code /public/html/**}
     *       (stable assets where URL stability matters).</li>
     *   <li><b>Content hash</b> (SHA-256, 8 chars) for {@code /public/assets/**}
     *       (aggressive cache-busting for static assets).</li>
     * </ul>
     *
     * @return configured {@link VersionalResourceResolver}
     * @see FixedVersionStrategy
     * @see ContentHashVersionStrategy
     */
    private VersionalResourceResolver getVersionalResourceResolver() {
        return new VersionalResourceResolver()
                .addStrategy(new FixedVersionStrategy(currentYearMonthVersion()), "/public/html/**")
                .addStrategy(new ContentHashVersionStrategy("SHA-256", 8), "/public/assets/**");
    }
}
