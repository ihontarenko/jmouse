package org.jmouse.core.proxy;

import java.util.List;

public interface InterceptorProvider {
    List<MethodInterceptor> getInterceptors(Class<?> criteria);
}
