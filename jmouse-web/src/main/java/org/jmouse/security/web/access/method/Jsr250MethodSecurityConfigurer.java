package org.jmouse.security.web.access.method;

import jakarta.annotation.security.RolesAllowed;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.core.proxy.InterceptorRegistry;
import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.ProxyFactory;
import org.jmouse.security.authorization.method.AuthorizeMethodInterceptor;
import org.jmouse.security.authorization.method.AuthorizeMethodManager;
import org.jmouse.web.mvc.BeanConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jmouse.core.proxy.InterceptorMatcher.forAnnotations;

/**
 * 🔐 {@link BeanConfigurer} that enables JSR-250 method-level security.
 *
 * <p>
 * Registers a {@link MethodInterceptor} into the {@link ProxyFactory} for methods
 * annotated with {@link RolesAllowed}. The interceptor delegates authorization
 * decisions to {@link AuthorizeMethodManager}.
 * </p>
 *
 * <p>
 * This configurer acts as a Jakarta / JSR-250 integration layer on top of the
 * framework's internal method-authorization mechanism.
 * </p>
 *
 * <p>
 * Typically activated conditionally when {@code jakarta.annotation.security.RolesAllowed}
 * is present on the classpath and the corresponding feature is enabled.
 * </p>
 */
@Bean
public class Jsr250MethodSecurityConfigurer implements BeanConfigurer<ProxyFactory> {

    /**
     * Logger for method security configuration events.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Jsr250MethodSecurityConfigurer.class);

    /**
     * Configures the given {@link ProxyFactory} by registering the JSR-250
     * method-security interceptor.
     *
     * <p>
     * Processing:
     * </p>
     * <ul>
     *     <li>creates a shared {@link AuthorizeMethodInterceptor}</li>
     *     <li>registers it for {@link RolesAllowed}</li>
     *     <li>uses negative order to ensure early interception</li>
     * </ul>
     *
     * @param proxyFactory proxy factory to configure
     */
    @Override
    public void configure(ProxyFactory proxyFactory) {
        InterceptorRegistry registry    = proxyFactory.getRegistry();
        MethodInterceptor   interceptor = new AuthorizeMethodInterceptor(new AuthorizeMethodManager());

        registry.register(interceptor, forAnnotations(RolesAllowed.class), -100);

        LOGGER.info(
                "Enabled JSR-250 method security interceptor → annotation: [@{}], interceptor: {}, order: {}",
                RolesAllowed.class.getSimpleName(),
                interceptor.getClass().getSimpleName(),
                -100
        );
    }

}