package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.core.UserPrincipal;
import org.jmouse.security.core.UserPrincipalService;
import org.jmouse.security.core.service.InMemoryUserPrincipalService;
import org.jmouse.security.web.SecurityFilterChain;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.web.http.HttpMethod;

@BeanFactories
public class SecurityConfiguration {

    @Bean
    public UserPrincipalService principalService() {
        UserPrincipal user = UserPrincipal.User.builder()
                .username("user")
                .password("password")
                .authority("ROLE_USER")
                .enabled(true)
                .build();
        UserPrincipal admin = UserPrincipal.User.builder()
                .username("admin")
                .password("admin")
                .authorities("ROLE_ADMIN", "ROLE_SUPER", "REBOOT")
                .enabled(true)
                .build();
        return new InMemoryUserPrincipalService(user, admin);
    }

    @Bean
    public SecurityFilterChain defaultFilterChain(
            HttpSecurity http, UserPrincipalService principalService) throws Exception {

        http.chainMatcher(matcher -> matcher.pathPattern("/**"));

        http.submitForm(form -> form
                .usernameParameter("username")
                .passwordParameter("password")
                .loginPage("/login/index")
                .redirect(r -> r.url("/asd"))
                .processing()
                    .formAction("/login/process")
                    .httpMethod(HttpMethod.POST)
//                .requestMatcher(r -> r.contentType())
        );

//        http.exceptionHandling(e -> e
//                .authenticationEntryPoint());

//        http.httpBasic(basic -> basic
//                .requestMatcher("/basic/**")
//                .enableChallengeOnFailure()
//        );

        http.authorizeHttpRequests(a -> a
                .requestPath("/login/**").permitAll()
                .requestPath("/shared/**").permitAll()
                .anyRequest().authenticated()
        );

        return http.build();
    }

}
