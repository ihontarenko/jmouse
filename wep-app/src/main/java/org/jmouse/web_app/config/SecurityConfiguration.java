package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.SecurityFilterChain;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpMethod;

@BeanFactories
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity http, WebBeanContext context) throws Exception {
        http.chainMatcher(RequestMatcher.pathPattern("/**"));

        http.securityContext(Customizer.defaults());

        http.submitForm(form -> form
                .usernameParameter("username")
                .passwordParameter("password")
                .loginPage("/login-internal")
                .processing()
                    .formAction("/login")
                    .httpMethod(HttpMethod.POST)
                .requestMatcher(RequestMatcher.pathPattern("/login").and(RequestMatcher.httpMethod(HttpMethod.POST)))
        );

        http.httpBasic(basic -> basic
                .requestMatcher("/basic/**")
        );

        return http.build();
    }

}
