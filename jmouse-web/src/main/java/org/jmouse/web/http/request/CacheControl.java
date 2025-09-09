package org.jmouse.web.http.request;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 🗄️ Builder-style representation of HTTP {@code Cache-Control} header.
 *
 * <p>Provides fluent methods to configure caching directives such as:</p>
 * <ul>
 *   <li>⏳ {@code max-age}, {@code s-maxage}</li>
 *   <li>🔄 {@code stale-while-revalidate}, {@code stale-if-error}</li>
 *   <li>🚫 {@code no-cache}, {@code no-store}, {@code no-transform}</li>
 *   <li>🔒 {@code private}, 🌍 {@code public}</li>
 *   <li>📌 {@code must-revalidate}, {@code proxy-revalidate}, {@code immutable}</li>
 * </ul>
 *
 * <p>Example:</p>
 * <pre>{@code
 * CacheControl cc = CacheControl.empty()
 *     .maxAge(1, TimeUnit.HOURS)
 *     .cachePublic()
 *     .immutable();
 *
 * String headerValue = cc.toHeaderValue();
 * }</pre>
 */
@SuppressWarnings("unused")
public class CacheControl {

    private Duration maxAge;
    private Duration staleWhileRevalidate;
    private Duration staleIfError;
    private Duration sMaxAge;
    private boolean  noCache         = false;
    private boolean  noStore         = false;
    private boolean  mustRevalidate  = false;
    private boolean  noTransform     = false;
    private boolean  cachePublic     = false;
    private boolean  cachePrivate    = false;
    private boolean  proxyRevalidate = false;
    private boolean  immutable       = false;

    /**
     * 🏗️ Create an empty cache control (no directives).
     */
    public static CacheControl empty() {
        return new CacheControl();
    }

    /**
     * 📑 Convert this configuration into an HTTP header value.
     *
     * @return string with comma-separated cache directives
     */
    public String toHeaderValue() {
        List<String> directives = new ArrayList<>();

        if (this.maxAge != null) {
            directives.add("max-age=" + this.maxAge.getSeconds());
        }
        if (this.noCache) {
            directives.add("no-cache");
        }
        if (this.noStore) {
            directives.add("no-store");
        }
        if (this.mustRevalidate) {
            directives.add("must-revalidate");
        }
        if (this.noTransform) {
            directives.add("no-transform");
        }
        if (this.cachePublic) {
            directives.add("public");
        }
        if (this.cachePrivate) {
            directives.add("private");
        }
        if (this.proxyRevalidate) {
            directives.add("proxy-revalidate");
        }
        if (this.sMaxAge != null) {
            directives.add("s-maxage=" + this.sMaxAge.getSeconds());
        }
        if (this.staleIfError != null) {
            directives.add("stale-if-error=" + this.staleIfError.getSeconds());
        }
        if (this.staleWhileRevalidate != null) {
            directives.add("stale-while-revalidate=" + this.staleWhileRevalidate.getSeconds());
        }
        if (this.immutable) {
            directives.add("immutable");
        }

        return String.join(", ", directives);
    }

    // ⏳ Max-age
    public CacheControl maxAge(long maxAge, TimeUnit unit) {
        return maxAge(Duration.ofSeconds(unit.toSeconds(maxAge)));
    }

    public CacheControl maxAge(Duration duration) {
        this.maxAge = duration;
        return this;
    }

    // 🔄 Stale-while-revalidate
    public CacheControl staleWhileRevalidate(long amount, TimeUnit unit) {
        return staleWhileRevalidate(Duration.ofSeconds(unit.toSeconds(amount)));
    }

    public CacheControl staleWhileRevalidate(Duration staleWhileRevalidate) {
        this.staleWhileRevalidate = staleWhileRevalidate;
        return this;
    }

    // 🔄 Stale-if-error
    public CacheControl staleIfError(long amount, TimeUnit unit) {
        return staleIfError(Duration.ofSeconds(unit.toSeconds(amount)));
    }

    public CacheControl staleIfError(Duration staleIfError) {
        this.staleIfError = staleIfError;
        return this;
    }

    // ⏳ S-maxage
    public CacheControl sMaxAge(long maxAge, TimeUnit unit) {
        return sMaxAge(Duration.ofSeconds(unit.toSeconds(maxAge)));
    }

    public CacheControl sMaxAge(Duration sMaxAge) {
        this.sMaxAge = sMaxAge;
        return this;
    }

    // 🚫 no-cache
    public CacheControl noCache(boolean noCache) {
        this.noCache = noCache;
        return this;
    }

    public CacheControl noCache() {
        return noCache(true);
    }

    // 🚫 no-store
    public CacheControl noStore(boolean noStore) {
        this.noStore = noStore;
        return this;
    }

    public CacheControl noStore() {
        return noStore(true);
    }

    // 📌 must-revalidate
    public CacheControl mustRevalidate(boolean mustRevalidate) {
        this.mustRevalidate = mustRevalidate;
        return this;
    }

    public CacheControl mustRevalidate() {
        return mustRevalidate(true);
    }

    // 🚫 no-transform
    public CacheControl noTransform(boolean noTransform) {
        this.noTransform = noTransform;
        return this;
    }

    public CacheControl noTransform() {
        return noTransform(true);
    }

    // 🌍 public
    public CacheControl cachePublic(boolean cachePublic) {
        this.cachePublic = cachePublic;
        return this;
    }

    public CacheControl cachePublic() {
        return cachePublic(true);
    }

    // 🔒 private
    public CacheControl cachePrivate(boolean cachePrivate) {
        this.cachePrivate = cachePrivate;
        return this;
    }

    public CacheControl cachePrivate() {
        return cachePrivate(true);
    }

    // 🔄 proxy-revalidate
    public CacheControl proxyRevalidate(boolean proxyRevalidate) {
        this.proxyRevalidate = proxyRevalidate;
        return this;
    }

    public CacheControl proxyRevalidate() {
        return proxyRevalidate(true);
    }

    // 📌 immutable
    public CacheControl immutable(boolean immutable) {
        this.immutable = immutable;
        return this;
    }

    public CacheControl immutable() {
        return immutable(true);
    }

    @Override
    public String toString() {
        return "CacheControl [%s]".formatted(toHeaderValue());
    }
}
