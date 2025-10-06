package org.jmouse.security.web.configuration;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.web.SecurityFilterChainDelegator;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.servlet.registration.FilterRegistrationBean;

@BeanFactories
public class HttpSecurityConfiguration implements InitializingBeanSupport<WebBeanContext> {

    private WebBeanContext context;

    @Bean(scope = BeanScope.PROTOTYPE, value = "httpSecurity")
    public HttpSecurity httpSecurity() {
        HttpSecurity httpSecurity = new HttpSecurity();

        httpSecurity.setSharedObject(WebBeanContext.class, context);

        return httpSecurity;
    }

//    @Bean
//    public FilterRegistrationBean<SecurityFilterChainDelegator> securityFilterRegistration(
//            SecurityFilterChainDelegator delegator) {
//        FilterRegistrationBean<SecurityFilterChainDelegator> registration = new FilterRegistrationBean<>(delegator);
//        registration.setOrder(-100500);
//        registration.addUrlPatterns("/*");
//        return registration;
//    }

    @Override
    public void initialize(WebBeanContext context) {
        this.context = context;
    }

}
