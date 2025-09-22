package org.jmouse.core.proxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 📚 Registry of {@link MethodInterceptor}s with optional ordering and matching rules.
 *
 * <p>Acts as a central place for collecting and selecting interceptors
 * to be applied to proxied classes.</p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>➕ Register interceptors with optional {@code order} (lower comes first).</li>
 *   <li>🎯 Attach {@link InterceptorMatcher} to restrict which target classes an interceptor applies to.</li>
 *   <li>🔎 Retrieve interceptors relevant to a given target type via {@link #select(Class)}.</li>
 * </ul>
 *
 * <pre>{@code
 * InterceptorRegistry registry = new InterceptorRegistry();
 * registry.register(new LoggingInterceptor(), 0);
 * registry.register(new TransactionInterceptor(), InterceptorMatcher.subtypeOf(Service.class), 10);
 *
 * List<MethodInterceptor> interceptors = registry.select(MyService.class);
 * }</pre>
 */
public final class InterceptorRegistry {

    private final List<Entry> entries = new ArrayList<>();

    /**
     * Register an interceptor with default order ({@code -1}) and no type restrictions.
     *
     * @param interceptor interceptor to register
     */
    public void register(MethodInterceptor interceptor) {
        register(interceptor, -1);
    }

    /**
     * Register an interceptor with given order and no type restrictions.
     *
     * @param interceptor interceptor to register
     * @param order       ordering hint (lower values applied earlier)
     */
    public void register(MethodInterceptor interceptor, int order) {
        register(interceptor, InterceptorMatcher.any(), order);
    }

    /**
     * Register an interceptor with a matcher and explicit order.
     *
     * @param interceptor interceptor to register
     * @param matcher     matcher to determine which target classes this applies to
     * @param order       ordering hint (lower values applied earlier)
     */
    public void register(MethodInterceptor interceptor, InterceptorMatcher matcher, int order) {
        entries.add(new Entry(interceptor, matcher, order));
        entries.sort(Comparator.comparingInt(Entry::order));
    }

    /**
     * Select interceptors applicable to the given target class,
     * in ascending order of registration {@code order}.
     *
     * @param type target class
     * @return matching interceptors
     */
    public List<MethodInterceptor> select(Class<?> type) {
        List<MethodInterceptor> selected = new ArrayList<>();

        for (Entry entry : entries) {
            if (entry.matcher.matches(type)) {
                selected.add(entry.interceptor);
            }
        }

        return selected;
    }

    /**
     * Internal record representing a registry entry.
     */
    private record Entry(MethodInterceptor interceptor, InterceptorMatcher matcher, int order) {}
}
