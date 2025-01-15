package svit.proxy;

import java.util.List;

public interface InterceptorProvider {
    List<MethodInterceptor> getInterceptors(Class<?> criteria);
}
