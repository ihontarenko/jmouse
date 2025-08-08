package org.jmouse.web_app.controller;

import org.jmouse.mvc.HandlerMappingException;
import org.jmouse.mvc.Model;
import org.jmouse.mvc.mapping.annotation.*;
import org.jmouse.web.request.http.HttpHeader;
import org.jmouse.web.request.http.HttpMethod;

@Controller
public class IndexController {

    @Mapping(path = "/users/{id}", httpMethod = HttpMethod.GET)
    public String index() {
        return "index/home";
    }

    @GetMapping(
            requestPath = "/demo/{id}",
            queryParameters = {@QueryParameter(name = "lang", value = "uk")},
            headers = {@Header(name = "X-TEXT", value = "HELLO")}
    )
    @MethodDescription("Demo Endpoint!")
    @ViewMapping("index/demo")
    public String demo(@PathVariable("id") Long id, Model model,
                       @RequestHeader(HttpHeader.USER_AGENT) String userAgent,
                       @RequestMethod HttpMethod method,
                       @RequestParameter("lang") String lang,
                       @RequestParameter("externalId") Long externalId) {

        model.addAttribute("ID", id);
        model.addAttribute("userAgent", userAgent);
        model.addAttribute("method", method);
        model.addAttribute("lang", lang);

        return "view -> index/demo";
    }

    @GetMapping(requestPath = "/welcome/{id}")
    public String hello(Class<?> type, @PathVariable("aaa") String name, @RequestParameter("lang") String lang) {
        return "index/demo";
    }

    @ExceptionHandler(HandlerMappingException.class)
    public String exceptionHandler() {
        return "index/error";
    }

}
