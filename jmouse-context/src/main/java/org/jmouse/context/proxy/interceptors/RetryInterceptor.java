package org.jmouse.context.proxy.interceptors;

import org.jmouse.context.proxy.api.MethodInterceptor;
import org.jmouse.context.proxy.api.MethodInvocation;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Basic retry with max attempts and backoff strategy.
 */
public final class RetryInterceptor implements MethodInterceptor {

    public interface Backoff {
        long sleepMillis(int attemptIndex); // attemptIndex starts at 1 for the first retry
        static Backoff fixed(long ms) { return i -> ms; }
        static Backoff exponential(long baseMs) { return i -> baseMs * (1L << (i - 1)); }
        static Backoff jitter(Backoff base) {
            java.util.Random rnd = new java.util.Random();
            return i -> (long) (base.sleepMillis(i) * (0.5 + rnd.nextDouble()));
        }
    }

    private final int maxAttempts;
    private final Backoff backoff;
    private final Predicate<Throwable> retryOn;

    public RetryInterceptor(int maxAttempts, Backoff backoff, Predicate<Throwable> retryOn) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts >= 1");
        this.maxAttempts = maxAttempts;
        this.backoff = Objects.requireNonNull(backoff);
        this.retryOn = Objects.requireNonNull(retryOn);
    }

    @Override
    public Object invoke(MethodInvocation inv) throws Throwable {
        int attempt = 0;
        Throwable last = null;
        while (attempt < maxAttempts) {
            try {
                return inv.proceed();
            } catch (Throwable t) {
                last = t;
                attempt++;
                if (attempt >= maxAttempts || !retryOn.test(t)) break;
                Thread.sleep(backoff.sleepMillis(attempt));
            }
        }
        throw last;
    }
}