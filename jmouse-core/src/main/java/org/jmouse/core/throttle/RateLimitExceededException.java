package org.jmouse.core.throttle;

import org.jmouse.core.throttle.AbstractRateLimitMethodInterceptor.RateLimitConfiguration;

public class RateLimitExceededException extends RuntimeException {

    private final RateLimitConfiguration configuration;

    public RateLimitExceededException(String message, RateLimitConfiguration configuration) {
        super(message);
        this.configuration = configuration;
    }

    public RateLimitConfiguration getRateLimitConfiguration() {
        return configuration;
    }

}
