package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.UserPrincipalService;
import org.jmouse.security.access.RoleHierarchy;
import org.jmouse.security.service.InMemoryUserPrincipalService;
import org.jmouse.security.web.MatchableSecurityFilterChain;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.security.web.configuration.configurer.AuthorizeRequestConfigurer;
import org.jmouse.web.http.HttpMethod;

import static org.jmouse.web.match.routing.MatcherCriteria.GET;
import static org.jmouse.web.match.routing.MatcherCriteria.POST;

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
    public MatchableSecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {

        http.chainMatcher(matcher -> matcher.pathPattern("/**"));
        http.principalService(principalService());

        http.authorization(this::authorizationConfiguration);

        http.authentication(a -> a
                .submitForm(form -> form
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .loginPage("/login/index")
                        .processing()
                        .formAction("/login/process")
                        .httpMethod(HttpMethod.POST)
                        .redirect(r -> r.url("/asd"))
                )
        );

        http.exceptionHandling(e -> e
                .accessDeniedHandler((request, response, exception) -> {
                    response.getWriter().println("forbidden!");
                    response.getWriter().println(exception.getMessage());
                    response.setStatus(403);
                })
        );

        return http.build();
    }

    private void authorizationConfiguration(AuthorizeRequestConfigurer<?>.AuthorizationRequestMatchCriterion configurer) {
        configurer
                .matcherCriteria(c -> c.pathPattern("/shared/**")).permitAll()
                .mappingMatcher(POST("/**"), GET("/health/**")).permitAll()
                .anyRequest().authenticated()
                .rolePrefix("R_")
                .roleHierarchy(RoleHierarchy.parse("R_ADMIN > R_OWNER, R_USER"));
    }

}
