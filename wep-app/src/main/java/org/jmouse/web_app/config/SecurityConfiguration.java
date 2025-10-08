package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.web.SecurityFilterChain;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpMethod;

@BeanFactories
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity http, WebBeanContext context) throws Exception {
        http.submitForm(form ->
                form.usernameParameter("username").passwordParameter("password")
                        .processing().formAction("/login")
        );
        return http.build();
    }

}
