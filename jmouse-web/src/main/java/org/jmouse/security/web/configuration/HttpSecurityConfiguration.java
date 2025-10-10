package org.jmouse.security.web.configuration;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.authentication.AuthenticationManager;
import org.jmouse.security.authentication.ResolversAuthenticationManager;
import org.jmouse.security.authentication.dao.DaoAuthenticationResolver;
import org.jmouse.security.web.SecurityFilterChainDelegatorRegistration;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.web.context.WebBeanContext;

import java.util.List;

@BeanFactories
public class HttpSecurityConfiguration implements InitializingBeanSupport<WebBeanContext> {

    private WebBeanContext context;

    @Bean(scope = BeanScope.PROTOTYPE, value = "httpSecurity")
    public HttpSecurity httpSecurity() {
        HttpSecurity httpSecurity = new HttpSecurity();

        httpSecurity.setSharedObject(WebBeanContext.class, context);
        httpSecurity.setSharedObject(AuthenticationManager.class, new ResolversAuthenticationManager(
                List.of(
                        new DaoAuthenticationResolver()
                )
        ));

        httpSecurity
                .securityContext(Customizer.noop())
                .exceptionHandling(Customizer.noop())
                .anonymous(Customizer.noop());

        return httpSecurity;
    }

    @Bean
    public SecurityFilterChainDelegatorRegistration securityFilterChainDelegatorRegistration() {
        return new SecurityFilterChainDelegatorRegistration();
    }

    @Override
    public void initialize(WebBeanContext context) {
        this.context = context;
    }

}
