package org.jmouse.core.limits;

import java.lang.annotation.*;
import java.time.temporal.ChronoUnit;

/**
 * Defines a rate limit for a method.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Maximum number of calls allowed within the window (or tokens in bucket).
     */
    long max();

    /**
     * Window unit for convenience; token bucket will compute refill rate from (max, per).
     */
    ChronoUnit per();

    /**
     * Scope of the limit key.
     */
    Scope scope() default Scope.METHOD;

    /**
     * Optional name to distinguish multiple limits on the same scope.
     */
    String name() default "";

    enum Scope {

        /**
         * One bucket per method signature (static, across all instances).
         */
        METHOD,

        /**
         * One bucket per target instance (this).
         */
        TARGET,

        /**
         * One bucket per concrete class.
         */
        CLASS,

        /**
         * Custom key via RateLimitKeyResolver.
         */
        CUSTOM
    }
}
