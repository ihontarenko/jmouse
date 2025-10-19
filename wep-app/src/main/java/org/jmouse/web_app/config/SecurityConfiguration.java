package org.jmouse.web_app.config;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.security.core.UserPrincipal;
import org.jmouse.security.core.UserPrincipalService;
import org.jmouse.security.core.service.InMemoryUserPrincipalService;
import org.jmouse.security.web.MatchableSecurityFilterChain;
import org.jmouse.security.web.configuration.Customizer;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.security.web.configuration.configurer.AuthorizeHttpRequestsConfigurer;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.match.Route;
import org.jmouse.web.match.routing.MappingCriteria;

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
                .anonymous(Customizer.noop())
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

        return http.build();
    }

    private void authorizationConfiguration(AuthorizeHttpRequestsConfigurer<?>.AuthorizationConfigurer configurer) {
        configurer.matcherCriteria(
                c -> c.pathPattern("/shared/**")
        ).permitAll()
        .mappingMatcher(
                MappingCriteria.POST("/**"),
                MappingCriteria.GET("/health/**")
        ).permitAll()
        .anyRequest().authenticated();
    }

}
