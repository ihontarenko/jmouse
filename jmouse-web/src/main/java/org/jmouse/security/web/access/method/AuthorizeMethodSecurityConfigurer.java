package org.jmouse.security.web.access.method;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.core.proxy.InterceptorMatcher;
import org.jmouse.core.proxy.InterceptorRegistry;
import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.ProxyFactory;
import org.jmouse.security.access.annotation.Authorize;
import org.jmouse.security.authorization.method.AuthorizeMethodInterceptor;
import org.jmouse.security.authorization.method.AuthorizeMethodManager;
import org.jmouse.web.mvc.BeanConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Bean
public class AuthorizeMethodSecurityConfigurer implements BeanConfigurer<ProxyFactory> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizeMethodSecurityConfigurer.class);

    @Override
    public void configure(ProxyFactory proxyFactory) {
        InterceptorRegistry registry    = proxyFactory.getRegistry();
        MethodInterceptor   interceptor = new AuthorizeMethodInterceptor(new AuthorizeMethodManager());

        registry.register(interceptor, InterceptorMatcher.forAnnotations(Authorize.class), -100);

        LOGGER.info("Enabled native method security interceptor for annotation: [@{}], order: {}",
                    Authorize.class.getSimpleName(),
                    -100
        );
    }

}
