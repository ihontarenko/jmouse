package org.jmouse.security.web.access.method;

import jakarta.annotation.security.RolesAllowed;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.core.proxy.InterceptorRegistry;
import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.ProxyFactory;
import org.jmouse.security.authorization.method.AuthorizeMethodInterceptor;
import org.jmouse.security.authorization.method.AuthorizeMethodManager;
import org.jmouse.security.access.annotation.Authorize;
import org.jmouse.web.mvc.BeanConfigurer;

import static org.jmouse.core.proxy.InterceptorMatcher.forAnnotations;

@Bean
public class MethodSecurityInterceptorConfigurer implements BeanConfigurer<ProxyFactory> {

    @Override
    public void configure(ProxyFactory proxyFactory) {
        InterceptorRegistry registry    = proxyFactory.getRegistry();
        MethodInterceptor   interceptor = new AuthorizeMethodInterceptor(new AuthorizeMethodManager());

        registry.register(interceptor, forAnnotations(Authorize.class), -100);
        registry.register(interceptor, forAnnotations(RolesAllowed.class), -100);
    }

}
