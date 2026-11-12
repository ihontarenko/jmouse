package org.jmouse.crawler.adapter.http;

import org.jmouse.http.client.HttpCall;
import org.jmouse.http.client.JdkHttpTransport;
import org.jmouse.core.Verify;

import java.time.Duration;

public final class HttpFetcherConfig {

    public static final String J_MOUSE_CRAWLER_UA = "jMouse-Crawler/1.0";

    private final Duration                   connectTimeout;
    private final Duration                   requestTimeout;
    private final JdkHttpTransport.Redirects redirects;
    private final String                     userAgent;
    private final long                       maximumResponseBytes;

    private HttpFetcherConfig(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.requestTimeout = builder.requestTimeout;
        this.redirects = builder.redirects;
        this.userAgent = builder.userAgent;
        this.maximumResponseBytes = builder.maximumResponseBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    /** Whether a redirect is followed — as a browser does, by default — or answered as it is. */
    public JdkHttpTransport.Redirects redirects() {
        return redirects;
    }

    /** How much of a page is read at most; past it the fetch fails. */
    public long maximumResponseBytes() {
        return maximumResponseBytes;
    }

    public String userAgent() {
        return userAgent;
    }

    public static final class Builder {
        private Duration                   connectTimeout       = Duration.ofSeconds(10);
        private Duration                   requestTimeout       = Duration.ofSeconds(30);
        private JdkHttpTransport.Redirects redirects            = JdkHttpTransport.Redirects.NORMAL;
        private String                     userAgent            = J_MOUSE_CRAWLER_UA;
        private long                       maximumResponseBytes = HttpCall.USUAL_RESPONSE_LIMIT;

        public Builder connectTimeout(Duration value) {
            this.connectTimeout = Verify.nonNull(value, "connectTimeout");
            return this;
        }

        public Builder requestTimeout(Duration value) {
            this.requestTimeout = Verify.nonNull(value, "requestTimeout");
            return this;
        }

        public Builder redirects(JdkHttpTransport.Redirects value) {
            this.redirects = Verify.nonNull(value, "redirects");
            return this;
        }

        public Builder maximumResponseBytes(long value) {
            this.maximumResponseBytes = value;
            return this;
        }

        public Builder userAgent(String value) {
            this.userAgent = Verify.nonNull(value, "userAgent");
            return this;
        }

        public HttpFetcherConfig build() {
            return new HttpFetcherConfig(this);
        }
    }
}
