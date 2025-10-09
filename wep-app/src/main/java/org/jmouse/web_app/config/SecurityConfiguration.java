package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.web.RequestMatcher;
import org.jmouse.security.web.SecurityFilterChain;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpMethod;

import static org.jmouse.security.web.RequestMatcher.httpMethod;
import static org.jmouse.security.web.RequestMatcher.pathPattern;

@BeanFactories
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity http, WebBeanContext context) throws Exception {
        http.chainMatcher(pathPattern("/**"));

        http.setSharedObject(WebBeanContext.class, context);

        http.securityContext(Customizer.noop());

        http.submitForm(form -> form
                .usernameParameter("username")
                .passwordParameter("password")
                .loginPage("/login-internal")
                .processing()
                    .formAction("/login")
                    .httpMethod(HttpMethod.POST)
                .requestMatcher(pathPattern("/login").and(httpMethod(HttpMethod.POST)))
        );

        http.httpBasic(basic -> basic
                .requestMatcher("/basic/**")
                .enableChallengeOnFailure()
        );

        return http.build();
    }

}
