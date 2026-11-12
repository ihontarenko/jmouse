package org.jmouse.showcase;

import java.sql.SQLException;
import java.util.List;
import org.apache.catalina.startup.Tomcat;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.Customizer;
import org.jmouse.http.Allow;
import org.jmouse.http.HttpMethod;
import org.jmouse.jdbc.JdbcAccess;
import org.jmouse.jdbc.connection.datasource.DriverManagerDataSource;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.password.PasswordEncoder;
import org.jmouse.security.password.Pbkdf2Sha256PasswordEncoder;
import org.jmouse.security.service.InMemoryUserPrincipalService;
import org.jmouse.security.web.SecurityFilterChainDelegator;
import org.jmouse.security.web.authentication.LogoutAuthenticationFilter;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.security.web.csrf.CsrfFilter;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.initializer.WebApplicationInitializer;
import org.jmouse.web.mvc.mapping.ResourceHttpMapping;
import org.jmouse.web.mvc.resource.LocationScanningResolver;
import org.jmouse.web.mvc.resource.PathNormalizationResolver;
import org.jmouse.web.mvc.resource.ResourceHandlerRegistry;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerFactory;
import org.jmouse.web.server.tomcat.TomcatWebServerFactory;
import org.jmouse.web.servlet.registration.FilterRegistrationBean;

@BeanFactories
public class ShowcaseConfiguration implements InitializingBeanSupport<WebBeanContext> {

    @Bean("webServerFactory")
    public WebServerFactory webServerFactory() {
        return new TomcatWebServerFactory() {
            @Override
            public WebServer createWebServer(WebApplicationInitializer... initializers) {
                WebServer server = super.createWebServer(initializers);
                ((Tomcat) server.server()).getConnector().setProperty("address", "127.0.0.1");
                return server;
            }
        };
    }

    @Bean
    public JdbcAccess jdbcAccess(ShowcaseSettings settings) {
        return JdbcAccess.from(
            new DriverManagerDataSource(settings.jdbcUrl(), "showcase", settings.secret("database.password"))
        );
    }

    @Bean
    public ShowcaseStore showcaseStore(JdbcAccess access) throws SQLException {
        return new ShowcaseStore(access);
    }

    @Bean
    public FilterRegistrationBean<SecurityFilterChainDelegator> security(ShowcaseSettings settings)
        throws Exception {
        PasswordEncoder encoder = new Pbkdf2Sha256PasswordEncoder();
        UserPrincipal member = createUser("demo", settings.secret("user.password"), encoder, "ROLE_USER");
        UserPrincipal administrator = createUser(
            "admin",
            settings.secret("admin.password"),
            encoder,
            "ROLE_USER",
            "ROLE_ADMIN"
        );
        HttpSecurity http = new HttpSecurity();
        http.principalService(
            new InMemoryUserPrincipalService(encoder, member, administrator)
        ).passwordEncoder(encoder);
        http.sessionManagement(session -> session.policy(SessionCreationPolicy.IF_REQUIRED));
        http.securityContext(Customizer.noop());
        http.exceptionHandling(exceptions ->
            exceptions.authenticationEntryPoint((request, response, failure) -> response.setStatus(401))
        );
        configureFormLogin(http);
        http.addFilterAfter(new LogoutAuthenticationFilter(), CsrfFilter.class);
        configureAuthorization(http);

        var registration = new FilterRegistrationBean<>(
            "showcaseSecurity",
            new SecurityFilterChainDelegator(List.of(http.build()))
        );
        registration.setEnabled(true);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Override
    public void doInitialize(WebBeanContext context) {
        ResourceHandlerRegistry registry = context.getBean(ResourceHandlerRegistry.class);
        registry
            .registerHandler("/static/**")
            .addResourceLocations("classpath:static/")
            .setAllow(Allow.of(HttpMethod.GET, HttpMethod.HEAD))
            .getChainRegistration()
            .addResolvers(new PathNormalizationResolver(), new LocationScanningResolver());
        context.getBean(ResourceHttpMapping.class).handleRegistrations(registry.getRegistrations());
    }

    private static UserPrincipal createUser(
        String username,
        String password,
        PasswordEncoder encoder,
        String... authorities
    ) {
        var user = UserPrincipal.User.builder()
            .username(username)
            .password(encoder.encode(password.toCharArray()));
        for (String authority : authorities) {
            user.authority(authority);
        }
        return user.build();
    }

    private static void configureFormLogin(HttpSecurity http) {
        http.authentication(authentication ->
            authentication.submitForm(form -> {
                form.disableDefaultLoginPage().usernamePassword("username", "password");
                form.processing().formAction("/login").post();
                form.successHandler((request, response) -> response.setStatus(204));
                form.failureHandler((request, response, failure) -> response.setStatus(401));
            })
        );
    }

    private static void configureAuthorization(HttpSecurity http) {
        http.authorization(authorize -> {
            authorize.requestPath("/api/security/admin").hasRole("ADMIN");
            authorize.requestPath("/api/security/member").authenticated();
            authorize
                .matcherCriteria(criteria -> criteria.pathPattern("/api/entries").httpMethod(HttpMethod.POST))
                .authenticated();
            authorize.requestPath("/api/transfers").authenticated();
            authorize.anyRequest().permitAll();
        });
    }
}
