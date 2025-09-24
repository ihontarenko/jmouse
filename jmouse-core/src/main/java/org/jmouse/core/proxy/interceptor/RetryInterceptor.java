package org.jmouse.core.proxy.interceptor;

import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.MethodInvocation;

import java.util.Random;
import java.util.concurrent.locks.LockSupport;

/**
 * 🔁 Interceptor that retries failed method invocations according to a configurable
 * {@link Backoff} strategy and {@link RetryPolicy}.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>Retry transient failures (e.g. network issues, timeouts)</li>
 *   <li>Apply exponential or jitter backoff between retries</li>
 *   <li>Skip retries for specific exception types</li>
 * </ul>
 */
public class RetryInterceptor implements MethodInterceptor {

    private final Backoff     backoff;
    private final int         attempts; // e.g. 5
    private final RetryPolicy policy;

    /**
     * Creates a retry interceptor.
     *
     * @param attempts maximum number of retry attempts
     * @param backoff backoff strategy used between retries
     * @param policy retry policy (defaults to {@link RetryPolicy#always()} if {@code null})
     */
    public RetryInterceptor(int attempts, Backoff backoff, RetryPolicy policy) {
        this.backoff = backoff;
        this.attempts = attempts;
        this.policy = policy == null ? RetryPolicy.always() : policy;
    }

    /**
     * Sleeps the given duration in a precise manner using {@link LockSupport#parkNanos}.
     *
     * @param millis duration in milliseconds
     * @throws InterruptedException if the thread is interrupted
     */
    private static void sleep(long millis) throws InterruptedException {
        long deadline = System.nanoTime() + (millis * 1_000_000L);

        do {
            long remaining = deadline - System.nanoTime();

            if (remaining <= 0) {
                return;
            }

            LockSupport.parkNanos(remaining);

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        } while (true);
    }

    /**
     * Executes the target method with retry logic.
     *
     * @param invocation method invocation
     * @return result of the method
     * @throws Throwable if retries are exhausted or policy disallows retry
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        int attempt = 0;

        while (true) {
            try {
                return invocation.proceed();
            } catch (Throwable throwable) {
                if (attempt < getAttempts() && policy.shouldRetry(throwable, attempt)) {

                    long ms = Math.max(0L, backoff.sleep(attempt));

                    try {
                        sleep(ms);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw exception;
                    }

                    attempt++;

                    continue;
                }

                throw throwable;
            }
        }
    }

    /**
     * @return maximum number of retry attempts
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * ⏳ Backoff strategy for retry delays.
     */
    @FunctionalInterface
    public interface Backoff {

        /**
         * Fixed backoff.
         *
         * @param ms constant delay in ms
         * @return backoff strategy
         */
        static Backoff fixed(long ms) {
            return a -> ms;
        }

        /**
         * Exponential backoff.
         *
         * @param ms base delay in ms
         * @return backoff strategy
         */
        static Backoff exponential(long ms) {
            return a -> ms * (1L << (a - 1));
        }

        /**
         * Exponential backoff with max cap.
         *
         * @param ms base delay
         * @param max maximum delay
         * @return backoff strategy
         */
        static Backoff exponential(long ms, long max) {
            return a -> Math.max(exponential(ms).sleep(a), max);
        }

        /**
         * Adds jitter (randomization) to a base backoff.
         *
         * @param base base backoff
         * @return jittered backoff
         */
        static Backoff jitter(Backoff base) {
            return i -> (long) (base.sleep(i) * (0.5 + new Random().nextDouble()));
        }

        /**
         * @param attempts current attempt index
         * @return sleep time in ms
         */
        long sleep(int attempts);
    }

    /**
     * 🎯 Policy that decides whether to retry on a given exception.
     */
    @FunctionalInterface
    public interface RetryPolicy {

        /**
         * Always retry regardless of exception.
         */
        static RetryPolicy always() {
            return (t, a) -> true;
        }

        /**
         * Skip retries for {@link IllegalArgumentException}.
         */
        static RetryPolicy skipIllegalArgument() {
            return (throwable, attempts) -> !(throwable instanceof IllegalArgumentException);
        }

        /**
         * Skip retries for {@link IllegalStateException}.
         */
        static RetryPolicy skipIllegalState() {
            return (throwable, attempts) -> !(throwable instanceof IllegalStateException);
        }

        /**
         * Skip retries for {@link NullPointerException}.
         */
        static RetryPolicy skipNullPointer() {
            return (throwable, attempts) -> !(throwable instanceof NullPointerException);
        }

        /**
         * @param throwable exception thrown
         * @param attempt current attempt index
         * @return {@code true} if retry should continue
         */
        boolean shouldRetry(Throwable throwable, int attempt);
    }
}
