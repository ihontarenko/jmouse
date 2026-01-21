package org.jmouse.crawler.adapter.http;

import org.jmouse.core.Verify;

import java.time.Duration;

public final class HttpFetcherConfig {

    public static final String J_MOUSE_CRAWLER_UA = "jMouse-Crawler/1.0";

    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final boolean  followRedirects;
    private final String   userAgent;

    private HttpFetcherConfig(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.requestTimeout = builder.requestTimeout;
        this.followRedirects = builder.followRedirects;
        this.userAgent = builder.userAgent;
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

    public boolean followRedirects() {
        return followRedirects;
    }

    public String userAgent() {
        return userAgent;
    }

    public static final class Builder {
        private Duration connectTimeout  = Duration.ofSeconds(10);
        private Duration requestTimeout  = Duration.ofSeconds(30);
        private boolean  followRedirects = true;
        private String   userAgent       = J_MOUSE_CRAWLER_UA;

        public Builder connectTimeout(Duration value) {
            this.connectTimeout = Verify.nonNull(value, "connectTimeout");
            return this;
        }

        public Builder requestTimeout(Duration value) {
            this.requestTimeout = Verify.nonNull(value, "requestTimeout");
            return this;
        }

        public Builder followRedirects(boolean value) {
            this.followRedirects = value;
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
