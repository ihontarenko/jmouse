package org.jmouse.core.throttle;

import java.lang.annotation.*;
import java.time.temporal.ChronoUnit;

/**
 * ⏳ Annotation for declaring a rate limit on a method or type.
 *
 * <p>Supports both fixed-window and token-bucket semantics.
 * The container enforces max invocations within a time unit,
 * or derives token refill rate from {@code (max, per)}.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @RateLimit(max = 10, per = ChronoUnit.SECONDS, amount = 1)
 * public void sensitiveApi() { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 📊 Maximum allowed calls (or tokens) in the defined period.
     */
    long max();

    /**
     * 🕐 Time unit for the window.
     * <p>In token-bucket mode, refill rate is derived from {@code (max, per)}.</p>
     */
    ChronoUnit per();

    /**
     * 🔢 Amount of time units in the window.
     * <p>Example: {@code per = SECONDS, amount = 30} → 30-second window.</p>
     */
    long amount();

    /**
     * 🗝️ Scope of the rate-limit key.
     * <ul>
     *   <li>{@link Scope#METHOD} – one bucket per method signature (default).</li>
     *   <li>{@link Scope#TARGET} – one bucket per object instance.</li>
     *   <li>{@link Scope#CLASS} – one bucket per concrete class.</li>
     *   <li>{@link Scope#CUSTOM} – resolved via custom key resolver.</li>
     * </ul>
     */
    Scope scope() default Scope.METHOD;

    /**
     * 🏷️ Optional identifier to distinguish multiple limits on same scope.
     */
    String name() default "";

    /**
     * 📌 Supported rate-limit scopes.
     */
    enum Scope {
        /** 🔒 One bucket per method signature (static, across all instances). */
        METHOD,

        /** 🔒 One bucket per target instance ({@code this}). */
        TARGET,

        /** 🔒 One bucket per concrete class. */
        CLASS,

        /** 🔒 Custom key resolved by a {@code RateLimitKeyResolver}. */
        CUSTOM
    }
}
