package svit.proxy;

import svit.proxy.annotation.ProxyMethodInterceptor;
import svit.reflection.Reflections;

public class AnnotationProxyFactory extends ProxyFactory {

    public AnnotationProxyFactory(Object target, Class<?>... baseClasses) {
        super(target);
        scanInterceptors(baseClasses);
    }

    public AnnotationProxyFactory(Object target) {
        super(target);
    }

    public void scanInterceptors(Class<?>... baseClasses) {
        // todo: use ClassFinder instead own implementation
        for (Class<?> interceptorClass : InterceptorScanner.getMethodInterceptorClasses(baseClasses)) {
            ProxyMethodInterceptor annotation = interceptorClass.getAnnotation(ProxyMethodInterceptor.class);
            Class<?>[]             targets    = annotation.value();

            for (Class<?> targetClass : targets) {
                if (targetClass.isAssignableFrom(proxyConfig.getTargetClass())) {
                    MethodInterceptor interceptor = (MethodInterceptor) Reflections.instantiate(
                            Reflections.findFirstConstructor(interceptorClass));

                    addInterceptor(interceptor);
                }
            }
        }
    }

}