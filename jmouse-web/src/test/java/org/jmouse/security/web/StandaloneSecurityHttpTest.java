package org.jmouse.security.web;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import org.apache.catalina.startup.Tomcat;
import org.jmouse.core.Customizer;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.password.Pbkdf2Sha256PasswordEncoder;
import org.jmouse.security.service.InMemoryUserPrincipalService;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.web.server.tomcat.TomcatWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StandaloneSecurityHttpTest {

    @TempDir
    Path directory;

    @Test
    void basicAuthenticationAuthorizationAndCleanupWorkOverRealHttpWithoutMvc() throws Exception {
        var encoder = new Pbkdf2Sha256PasswordEncoder(1000, 16, 32);
        UserPrincipal user = UserPrincipal.User.builder()
            .username("user")
            .password(encoder.encode("secret".toCharArray()))
            .authority("ROLE_USER")
            .build();
        HttpSecurity http = new HttpSecurity();
        http.principalService(new InMemoryUserPrincipalService(encoder, user)).passwordEncoder(encoder);
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.policy(SessionCreationPolicy.STATELESS));
        http.securityContext(Customizer.noop());
        http.exceptionHandling(Customizer.noop());
        http.authentication(authentication -> authentication.httpBasic(Customizer.noop()));
        http.authorization(authorize -> {
            authorize.requestPath("/public").permitAll();
            authorize.requestPath("/admin").hasRole("ADMIN");
            authorize.anyRequest().authenticated();
        });
        var security = new SecurityFilterChainDelegator(List.of(http.build()));
        try (TomcatWebServer server = new TomcatWebServer(); HttpClient client = HttpClient.newHttpClient()) {
            server.server().setBaseDir(directory.toString());
            server.server().setPort(0);
            server.server().getConnector().setProperty("maxThreads", "1");
            server.server().getConnector().setProperty("minSpareThreads", "1");
            var context = server.server().addContext("", directory.toString());
            context.addServletContainerInitializer(
                (classes, servletContext) ->
                    servletContext
                        .addFilter("security", security)
                        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*"),
                java.util.Set.of()
            );
            Tomcat.addServlet(
                context,
                "application",
                new HttpServlet() {
                    protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws IOException, ServletException {
                        if (request.getServletPath().equals("/error")) {
                            throw new ServletException("application failure");
                        }
                        var authentication = SecurityContextHolder.getContext().getAuthentication();
                        response
                            .getWriter()
                            .write(authentication == null ? "anonymous" : authentication.getName());
                    }
                }
            );
            context.addServletMappingDecoded("/", "application");
            server.start();
            int port = server.port();
            var denied = get(client, port, "/private", null);
            assertEquals(401, denied.statusCode());
            assertTrue(denied.headers().firstValue("WWW-Authenticate").orElse("").startsWith("Basic"));
            String authorization =
                "Basic " +
                Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.ISO_8859_1));
            var allowed = get(client, port, "/private", authorization);
            assertEquals(200, allowed.statusCode());
            assertEquals("user", allowed.body());
            assertFalse(allowed.headers().firstValue("Set-Cookie").isPresent());
            assertEquals(403, get(client, port, "/admin", authorization).statusCode());
            assertEquals(401, get(client, port, "/private", "Basic broken").statusCode());
            assertEquals(500, get(client, port, "/error", authorization).statusCode());
            assertEquals("anonymous", get(client, port, "/public", null).body());
            assertEquals(401, get(client, port, "/private", null).statusCode());
        }
    }

    private HttpResponse<String> get(HttpClient client, int port, String path, String authorization)
        throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(5))
            .GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
