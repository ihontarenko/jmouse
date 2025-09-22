package org.jmouse.core.proxy;


import java.util.ArrayList;
import java.util.List;

public final class InterceptorRegistry {
    private final List<Entry> entries = new ArrayList<>();

    public void register(MethodInterceptor interceptor, InvocationMatcher matcher, int order) {
        entries.add(new Entry(interceptor, matcher, order));
        entries.sort(Comparator.comparingInt(Entry::order));
    }

    public List<MethodInterceptor> select(MethodInvocation inv) {
        List<MethodInterceptor> list = new ArrayList<>();
        for (Entry e : entries) if (e.matcher.matches(inv)) list.add(e.interceptor);
        return list;
    }

    private record Entry(MethodInterceptor interceptor, InvocationMatcher matcher, int order) {}
}
