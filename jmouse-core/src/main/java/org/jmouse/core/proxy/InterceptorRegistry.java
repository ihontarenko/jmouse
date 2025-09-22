package org.jmouse.core.proxy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InterceptorRegistry {

    private final List<Entry> entries = new ArrayList<>();

    public void register(MethodInterceptor interceptor) {
        register(interceptor, -1);
    }

    public void register(MethodInterceptor interceptor, int order) {
        register(interceptor, InvocationMatcher.any(), order);
    }

    public void register(MethodInterceptor interceptor, InvocationMatcher matcher, int order) {
        entries.add(new Entry(interceptor, matcher, order));
        entries.sort(Comparator.comparingInt(Entry::order));
    }

    public List<MethodInterceptor> select(MethodInvocation invocation) {
        List<MethodInterceptor> selected = new ArrayList<>();

        for (Entry entry : entries) {
            if (entry.matcher.matches(invocation)) {
                selected.add(entry.interceptor);
            }
        }

        return selected;
    }

    private record Entry(MethodInterceptor interceptor, InvocationMatcher matcher, int order) {}

}
