package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.web.SecurityFilterChain;
import org.jmouse.security.web.configuration.builder.HttpSecurity;

@BeanFactories
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http.basic(b -> b.requestMatcher());
        System.out.println("SecurityFilterChain");
        return http.build();
    }

    @Bean
    public SecurityFilterChain webResourcesFilterChain(HttpSecurity http) throws Exception {
        http.basic(b -> b.requestMatcher());
        System.out.println("SecurityFilterChain2");
        return http.build();
    }

}
