package org.jmouse.web_app.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.beans.annotation.ProxiedBean;
import org.jmouse.core.throttle.RateLimit;
import org.jmouse.web.binding.BindingResult;
import org.jmouse.web.binding.WebModel;
import org.jmouse.web.http.RequestParameters;
import org.jmouse.web.mvc.Model;
import org.jmouse.web.annotation.*;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.mvc.resource.ResourceUrlResolver;

import java.time.temporal.ChronoUnit;

@Controller
@ProxiedBean
public class IndexController {

    private ResourceUrlResolver resourceUrlResolver;

    @BeanConstructor
    public IndexController(ResourceUrlResolver resourceUrlResolver) {
        this.resourceUrlResolver = resourceUrlResolver;
    }

    @Mapping(path = "/users/{id}", httpMethod = HttpMethod.GET)
    public String index() {
        return "index/home";
    }

    @Mapping(path = "/video", httpMethod = HttpMethod.GET)
    public String video() {
        return "view:jmouse/video";
    }

    @Mapping(
            path = "/demo/{id}",
            queryParameters = {
                    @QueryParameter(name = "lang", value = "uk")
            },
            httpMethod = HttpMethod.POST
    )
    @MethodDescription("Demo Endpoint!")
    @ViewMapping("index/demo")
    @RateLimit(max = 4, per = ChronoUnit.SECONDS, amount = 10)
    public String demo(
            @PathVariable("id") Long id, Model model,
            @RequestHeader(HttpHeader.USER_AGENT) String userAgent,
            @RequestMethod HttpMethod method,
            @RequestParameter("lang") String lang,
            @RequestParameter("externalId") Long externalId,
            @RequestParameter("filter") Object filter,
            @WebModel("userDTO") User user,
            BindingResult<?> bindingResult,
            HttpServletRequest request
    ) {
        model.addAttribute("ID", id);
        model.addAttribute("userAgent", userAgent);
        model.addAttribute("method", method);
        model.addAttribute("lang", lang);
        model.addAttribute("externalId", externalId);

        RequestParameters parameters = RequestParameters.ofRequest(request);

        return "view:index/demo";
    }

    @GetMapping(requestPath = "/public/favicon", produces = {"text/plain", "application/json"})
    public String favicon() {
        return resourceUrlResolver.lookupResourceUrl("/internal/assets/icon/favicon.ico");
    }

    @Mapping(
            httpMethod = HttpMethod.GET,
            path = "/public/error/{id}",
            consumes = {"application/x-www-form-urlencoded"},
            produces = {"text/plain", "application/json"}
    )
    public String error() {
        throw new IllegalStateException("An some error occurred");
    }

    public record UserDTO(String name, @Email String email, @NotBlank String password) {}

    public static class User {

        @NotBlank(message = "name must not blank!")
        private String name;
        @NotBlank
        private String password;
        @Email
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

}
