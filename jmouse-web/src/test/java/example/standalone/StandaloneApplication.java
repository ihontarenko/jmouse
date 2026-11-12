package example.standalone;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.core.Customizer;
import org.jmouse.http.HttpMethod;
import org.jmouse.jdbc.JdbcAccess;
import org.jmouse.jdbc.connection.datasource.DriverManagerDataSource;
import org.jmouse.security.UserPrincipal;
import org.jmouse.security.password.Pbkdf2Sha256PasswordEncoder;
import org.jmouse.security.service.InMemoryUserPrincipalService;
import org.jmouse.security.web.SecurityFilterChainDelegator;
import org.jmouse.security.web.configuration.builder.HttpSecurity;
import org.jmouse.security.web.session.SessionCreationPolicy;
import org.jmouse.web.annotation.Controller;
import org.jmouse.web.annotation.Mapping;
import org.jmouse.web.annotation.PathVariable;
import org.jmouse.web.factories.WebServerFactoryConfiguration.WebServerConfigHolder;
import org.jmouse.web.mvc.WebApplicationLauncher;
import org.jmouse.web.server.WebServer;
import org.jmouse.web.server.WebServerConfig;
import org.jmouse.web.server.WebServers;
import org.jmouse.web.server.tomcat.TomcatWebServer;
import org.jmouse.web.servlet.registration.FilterRegistrationBean;

/**
 * Runnable MVC + JDBC + Basic-auth example without Spring. Not a JUnit test.
 * Supply an H2 driver at runtime and JMOUSE_DEMO_PASSWORD in the environment.
 * --verify uses an ephemeral port and closes the application after exercising the public API.
 */
public final class StandaloneApplication {

    public static void main(String[] arguments) throws Exception {
        String password = System.getenv("JMOUSE_DEMO_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Set JMOUSE_DEMO_PASSWORD before starting the example");
        }
        boolean verify = List.of(arguments).contains("--verify");
        int port = verify ? 0 : Integer.parseInt(System.getenv().getOrDefault("JMOUSE_DEMO_PORT", "0"));
        var context = new WebApplicationLauncher(StandaloneApplication.class)
            .configureContext(root -> {
                root.getBean(WebServerConfigHolder.class).setWebServerConfigs(
                    Map.of(
                        WebServers.TOMCAT,
                        new WebServerConfig(port, WebServers.TOMCAT, "target/standalone-example-runtime")
                    )
                );
                if (verify) {
                    root.getBean(
                        org.jmouse.web.mvc.servlet.FirewallBeanConfiguration.Properties.class
                    ).setTokenBucket(new org.jmouse.web.security.firewall.RateLimitProperties(100, 100));
                }
            })
            .launch(arguments);
        WebServer server = context.getBean(WebServer.class);
        if (verify) {
            try (server; HttpClient client = HttpClient.newHttpClient()) {
                String base = "http://localhost:" + ((TomcatWebServer) server).port();
                String authorization =
                    "Basic " +
                    Base64.getEncoder().encodeToString(
                        ("demo:" + password).getBytes(StandardCharsets.ISO_8859_1)
                    );
                expect(client, base, "/hello/jmouse", null, "GET", 200);
                expect(client, base, "/entries", null, "GET", 401);
                expect(client, base, "/entries", authorization, "GET", 200);
                expect(client, base, "/absent", authorization, "GET", 404);
                expect(client, base, "/hello/jmouse", authorization, "POST", 405);
                expectBody(
                    client,
                    base,
                    "/echo",
                    authorization,
                    "application/json",
                    "{\"name\":\"valid\"}",
                    200
                );
                expectBody(client, base, "/echo", authorization, "application/json", "{\"name\":\"\"}", 400);
                expectBody(client, base, "/echo", authorization, "application/json", "{", 400);
                expectBody(client, base, "/echo", authorization, "text/plain", "unsupported", 415);
                expectBody(
                    client,
                    base,
                    "/echo",
                    authorization,
                    "application/json",
                    "{\"name\":\"" + "x".repeat(512) + "\"}",
                    413
                );
                expectBody(
                    client,
                    base,
                    "/form",
                    authorization,
                    "application/x-www-form-urlencoded",
                    "input.name=valid",
                    200
                );
                expectBody(
                    client,
                    base,
                    "/form",
                    authorization,
                    "application/x-www-form-urlencoded",
                    "input.name=",
                    400
                );
                System.out.println("Standalone MVC/JDBC/Security example verified");
            }
        } else {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close, "Standalone-Shutdown"));
            System.out.println("Standalone application port: " + ((TomcatWebServer) server).port());
        }
    }

    private static void expect(
        HttpClient client,
        String base,
        String path,
        String authorization,
        String method,
        int status
    ) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json")
            .method(method, HttpRequest.BodyPublishers.noBody());
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != status) {
            throw new IllegalStateException(
                method + " " + path + ": expected " + status + ", received " + response.statusCode()
            );
        }
    }

    private static void expectBody(
        HttpClient client,
        String base,
        String path,
        String authorization,
        String contentType,
        String body,
        int status
    ) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/json")
            .header("Authorization", authorization)
            .header("Content-Type", contentType)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != status) {
            throw new IllegalStateException(
                "POST " + path + ": expected " + status + ", received " + response.statusCode()
            );
        }
    }

    public record Input(@jakarta.validation.constraints.NotBlank String name) {}

    @BeanFactories
    public static class Configuration {

        @Bean
        public JdbcAccess jdbcAccess() throws Exception {
            JdbcAccess access = JdbcAccess.from(
                new DriverManagerDataSource("jdbc:h2:mem:standalone;DB_CLOSE_DELAY=-1", "sa", "")
            );
            access.jdbc().update("create table entries (id integer)");
            access.jdbc().update("insert into entries values (1)");
            return access;
        }

        @Bean
        public FilterRegistrationBean<SecurityFilterChainDelegator> exampleSecurity() throws Exception {
            var encoder = new Pbkdf2Sha256PasswordEncoder();
            var user = UserPrincipal.User.builder()
                .username("demo")
                .password(encoder.encode(System.getenv("JMOUSE_DEMO_PASSWORD").toCharArray()))
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
                authorize.requestPath("/hello/{name}").permitAll();
                authorize.anyRequest().authenticated();
            });
            var registration = new FilterRegistrationBean<>(
                "exampleSecurity",
                new SecurityFilterChainDelegator(List.of(http.build()))
            );
            registration.setEnabled(true);
            registration.addUrlPatterns("/*");
            return registration;
        }
    }

    @Controller
    public static class Endpoints {

        private final JdbcAccess access;

        @BeanConstructor
        public Endpoints(JdbcAccess access) {
            this.access = access;
        }

        @Mapping(path = "/hello/{name}", httpMethod = HttpMethod.GET, produces = "application/json")
        public Map<String, String> hello(@PathVariable("name") String name) {
            return Map.of("hello", name);
        }

        @Mapping(
            path = "/echo",
            httpMethod = HttpMethod.POST,
            consumes = "application/json",
            produces = "application/json"
        )
        public Input echo(@org.jmouse.web.annotation.RequestBody(maxBytes = 256) Input input) {
            return input;
        }

        @Mapping(path = "/form", httpMethod = HttpMethod.POST, produces = "application/json")
        public Input form(@org.jmouse.web.binding.WebModel(value = "input", prefix = "input") Input input) {
            return input;
        }

        @Mapping(path = "/entries", httpMethod = HttpMethod.GET, produces = "application/json")
        public Map<String, Integer> entries() throws Exception {
            return Map.of(
                "count",
                access.jdbc().queryOne("select count(*) from entries", (row, index) -> row.getInt(1))
            );
        }
    }
}
