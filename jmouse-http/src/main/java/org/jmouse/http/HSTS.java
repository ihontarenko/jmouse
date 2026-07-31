package org.jmouse.http;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 🔒 HTTP Strict-Transport-Security (HSTS) builder.
 *
 * <p>Generates the {@code Strict-Transport-Security} header value
 * with configurable directives: {@code max-age}, {@code includeSubDomains},
 * and {@code preload}.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * // ⚙️ Manual
 * HSTS custom = HSTS.empty()
 *     .maxAge(180, TimeUnit.DAYS)
 *     .includeSubdomains();
 *
 * // ✅ Recommended best-practice
 * HSTS secure = HSTS.recommended();
 *
 * System.out.println(secure.toHttpHeader());   // STRICT_TRANSPORT_SECURITY
 * System.out.println(secure.toHeaderValue()); // max-age=31536000; include-subdomains; preload
 * }</pre>
 */
public class HSTS extends AbstractHeader {

    private Duration maxAge;
    private boolean  includeSubdomains = false;
    private boolean  preload           = false;

    protected HSTS(HttpHeader httpHeader) {
        super(httpHeader);
    }

    /**
     * 🏗️ Create an empty HSTS policy (no directives).
     */
    public static HSTS empty() {
        return new HSTS(HttpHeader.STRICT_TRANSPORT_SECURITY);
    }

    /**
     * ✅ Recommended secure HSTS policy.
     *
     * <p>Sets {@code max-age=31536000} (1 year), enables
     * {@code includeSubDomains} and {@code preload}.</p>
     *
     * @return secure HSTS configuration
     */
    public static HSTS recommended() {
        return empty().maxAge(Duration.ofDays(365)).includeSubdomains().preload();
    }

    /**
     * 🔗 Render directives into header value.
     *
     * @return header string like {@code max-age=31536000; include-subdomains}
     */
    @Override
    public String toHeaderValue() {
        List<String> directives = new ArrayList<>();

        if (this.maxAge != null) {
            directives.add("max-age=" + this.maxAge.getSeconds());
        }

        if (this.includeSubdomains) {
            directives.add("include-subdomains");
        }

        if (this.preload) {
            directives.add("preload");
        }

        return String.join("; ", directives);
    }

    /**
     * ⏳ Set max-age in given time unit.
     */
    public HSTS maxAge(long maxAge, TimeUnit unit) {
        return maxAge(Duration.ofSeconds(unit.toSeconds(maxAge)));
    }

    /**
     * ⏳ Set max-age using {@link Duration}.
     */
    public HSTS maxAge(Duration duration) {
        this.maxAge = duration;
        return this;
    }

    /**
     * 🌐 Enable/disable {@code includeSubDomains}.
     */
    public HSTS includeSubdomains(boolean includeSubdomains) {
        this.includeSubdomains = includeSubdomains;
        return this;
    }

    /**
     * 🚀 Enable/disable {@code preload}.
     */
    public HSTS preload(boolean preload) {
        this.preload = preload;
        return this;
    }

    /**
     * 🌐 Shortcut: enable {@code includeSubDomains}.
     */
    public HSTS includeSubdomains() {
        return includeSubdomains(true);
    }

    /**
     * 🚀 Shortcut: enable {@code preload}.
     */
    public HSTS preload() {
        return preload(true);
    }
}
