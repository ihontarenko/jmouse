package org.jmouse.web_app.controller;

import org.jmouse.mvc.mapping.annnotation.Controller;
import org.jmouse.mvc.mapping.annnotation.GetMapping;
import org.jmouse.mvc.mapping.annnotation.Mapping;
import org.jmouse.web.request.http.HttpMethod;

@Controller
public class IndexController {

    @Mapping(path = "/users/{id}", httpMethod = HttpMethod.GET)
    public String index() {
        return "index/home";
    }

    @GetMapping(requestPath = "/demo/{id}")
    public String demo() {
        return "index/demo";
    }

    @GetMapping(requestPath = "/welcome/{id}")
    public String hello() {
        return "index/demo";
    }

}
