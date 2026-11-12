package org.jmouse.showcase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.http.HttpMethod;
import org.jmouse.http.HttpStatus;
import org.jmouse.security.Authority;
import org.jmouse.security.SecurityContextHolder;
import org.jmouse.security.authentication.AuthenticationLevel;
import org.jmouse.security.web.csrf.CsrfToken;
import org.jmouse.web.annotation.Controller;
import org.jmouse.web.annotation.ExceptionHandler;
import org.jmouse.web.annotation.Mapping;
import org.jmouse.web.annotation.PathVariable;
import org.jmouse.web.annotation.RequestBody;
import org.jmouse.web.binding.WebModel;
import org.jmouse.web.mvc.HttpRequestException;
import org.jmouse.web.mvc.Model;
import org.jmouse.web.mvc.cors.CorsMapping;

@Controller
public class ShowcaseController {

    private final ShowcaseStore store;

    @BeanConstructor
    public ShowcaseController(ShowcaseStore store) {
        this.store = store;
    }

    public record EntryInput(@NotBlank @Size(max = 120) String name) {}

    public record TransferInput(
        @Positive long fromId,
        @Positive long toId,
        @Positive @Max(1000000) long amount,
        boolean rollback
    ) {}

    public record FormInput(@NotBlank String name) {}

    public record Route(String method, String path, String description, String access) {}

    @Mapping(path = "/", httpMethod = HttpMethod.GET)
    public void index(HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (InputStream input = getClass().getResourceAsStream("/static/index.html")) {
            if (input == null) {
                throw new FileNotFoundException("Showcase index.html");
            }
            input.transferTo(response.getOutputStream());
        }
    }

    @Scenario("Routes discovered from actual controller mappings")
    @Mapping(path = "/api/routes", httpMethod = HttpMethod.GET, produces = "application/json")
    public List<Route> routes() {
        List<Route> routes = new ArrayList<>();
        for (var method : ShowcaseController.class.getDeclaredMethods()) {
            Mapping mapping = method.getAnnotation(Mapping.class);

            Scenario scenario = method.getAnnotation(Scenario.class);
            if (mapping != null && scenario != null) {
                routes.add(
                    new Route(
                        mapping.httpMethod().name(),
                        mapping.path(),
                        scenario.value(),
                        scenario.access()
                    )
                );
            }
        }
        routes.add(new Route("POST", "/login", "Form login with session fixation protection", "CSRF"));
        routes.add(new Route("POST", "/logout", "Invalidate the authenticated session", "CSRF"));
        routes.sort(Comparator.comparing(Route::path).thenComparing(Route::method));
        return routes;
    }

    @Scenario("Path variable and JSON response; CORS allows https://example.test")
    @CorsMapping(origins = "https://example.test", methods = HttpMethod.GET)
    @Mapping(path = "/api/hello/{name}", httpMethod = HttpMethod.GET, produces = "application/json")
    public Map<String, String> hello(@PathVariable("name") String name) {
        return Map.of("hello", name);
    }

    @Scenario("Read persisted JDBC rows")
    @Mapping(path = "/api/entries", httpMethod = HttpMethod.GET, produces = "application/json")
    public List<ShowcaseStore.Entry> entries() throws SQLException {
        return store.entries();
    }

    @Scenario(value = "Validate JSON and insert with named SQL parameters", access = "Signed in + CSRF")
    @Mapping(
        path = "/api/entries",
        httpMethod = HttpMethod.POST,
        consumes = "application/json",
        produces = "application/json"
    )
    public List<ShowcaseStore.Entry> add(@RequestBody(maxBytes = 4096) EntryInput input) throws SQLException {
        return store.addEntry(input.name().strip());
    }

    @Scenario("Read account balances in minor units")
    @Mapping(path = "/api/accounts", httpMethod = HttpMethod.GET, produces = "application/json")
    public List<ShowcaseStore.Account> accounts() throws SQLException {
        return store.accounts();
    }

    @Scenario(value = "Atomic transfer or intentional failure after debit", access = "Signed in + CSRF")
    @Mapping(
        path = "/api/transfers",
        httpMethod = HttpMethod.POST,
        consumes = "application/json",
        produces = "application/json"
    )
    public ShowcaseStore.TransferResult transfer(@RequestBody(maxBytes = 4096) TransferInput input)
        throws SQLException {
        try {
            return store.transfer(input.fromId(), input.toId(), input.amount(), input.rollback());
        } catch (ShowcaseStore.TransferRejectedException failure) {
            throw new HttpRequestException(HttpStatus.BAD_REQUEST, failure.getMessage(), failure);
        }
    }

    @Scenario(value = "Form binding and validation", access = "CSRF")
    @Mapping(path = "/api/form", httpMethod = HttpMethod.POST, produces = "application/json")
    public FormInput form(@WebModel("input") FormInput input) {
        return input;
    }

    @Scenario("Inspect the current session identity")
    @Mapping(path = "/api/session", httpMethod = HttpMethod.GET, produces = "application/json")
    public Map<String, Object> session() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated =
            authentication != null &&
            authentication.isAuthenticated() &&
            authentication.level() != AuthenticationLevel.ANONYMOUS;
        return Map.of(
            "authenticated",
            authenticated,
            "username",
            authenticated ? authentication.getName() : "Guest",
            "authorities",
            authenticated
                ? authentication.getAuthorities().stream().map(Authority::authority).toList()
                : List.of()
        );
    }

    @Scenario("Acquire a session-bound CSRF token")
    @Mapping(path = "/api/csrf", httpMethod = HttpMethod.GET, produces = "application/json")
    public Map<String, String> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.REQUEST_ATTRIBUTE);
        return Map.of("token", token.value(), "headerName", token.headerName());
    }

    @Scenario(value = "Requires an authenticated user", access = "Signed in")
    @Mapping(path = "/api/security/member", httpMethod = HttpMethod.GET, produces = "application/json")
    public Map<String, String> member() {
        return Map.of(
            "access",
            "member",
            "username",
            SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }

    @Scenario(value = "Requires ROLE_ADMIN", access = "Admin")
    @Mapping(path = "/api/security/admin", httpMethod = HttpMethod.GET, produces = "application/json")
    public Map<String, String> admin() {
        return Map.of(
            "access",
            "admin",
            "username",
            SecurityContextHolder.getContext().getAuthentication().getName()
        );
    }

    @Scenario("Typed null return, rendered as JSON null")
    @Mapping(path = "/api/mvc/null", httpMethod = HttpMethod.GET, produces = "application/json")
    public EntryInput nullResult() {
        return null;
    }

    @Scenario("Exception handler keeps the request model and supplies its own return type")
    @Mapping(path = "/api/mvc/recover", httpMethod = HttpMethod.GET, produces = "application/json")
    public String recoverableFailure(Model model) {
        model.addAttribute("scenario", "request-model-retained");
        throw new DemonstrationFailure();
    }

    @ExceptionHandler(DemonstrationFailure.class)
    public Map<String, Object> recover(DemonstrationFailure failure, Model model) {
        return model.getAttributes();
    }

    public static final class DemonstrationFailure extends RuntimeException {}
}
