package org.jmouse.web_app.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.web.mvc.HandlerMappingException;
import org.jmouse.web.mvc.Model;
import org.jmouse.web.annotation.*;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.mvc.resource.ResourceUrlResolver;

import java.util.List;

@Controller
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
    public String demo(
           @PathVariable("id") Long id, Model model,
           @RequestHeader(HttpHeader.USER_AGENT) String userAgent,
           @RequestMethod HttpMethod method,
           @RequestParameter("lang") String lang,
           @RequestParameter("externalId") Long externalId,
           HttpServletRequest request
    ) {
        model.addAttribute("ID", id);
        model.addAttribute("userAgent", userAgent);
        model.addAttribute("method", method);
        model.addAttribute("lang", lang);
        model.addAttribute("externalId", externalId);
        return "view:index/demo";
    }

    @GetMapping(requestPath = "/welcome/{id}", produces = {"text/plain"})
    public String hello() {
        return resourceUrlResolver.lookupResourceUrl("/internal/assets/icon/favicon.ico");
    }

    @Mapping(httpMethod = HttpMethod.DELETE, path = "/welcome/{id}", produces = {"text/plain"})
    public String hello2() {
        return resourceUrlResolver.lookupResourceUrl("/internal/assets/icon/favicon.ico");
    }

}
