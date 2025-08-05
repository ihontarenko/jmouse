package org.jmouse.web_app.controller;

import org.jmouse.mvc.Model;
import org.jmouse.mvc.mapping.annnotation.*;
import org.jmouse.web.request.http.HttpMethod;

@Controller
public class IndexController {

    @Mapping(path = "/users/{id}", httpMethod = HttpMethod.GET)
    public String index() {
        return "index/home";
    }

    @GetMapping(
            requestPath = "/demo/{id}",
            queryParameters = {
                    @QueryParameter(name = "lang", value = "uk")
            },
            headers = {
                    @Header(name = "X-TEXT", value = "HELLO")
            }
    )
    @MethodDescription("Demo Endpoint!")
    @ViewMapping("ViewMapping/index/demo")
    public String demo(@PathVariable("id") Long id, Model model) {
        model.addAttribute("ID", id);
        return "index/demo";
    }

    @GetMapping(requestPath = "/welcome/{id}")
    public String hello(Class<?> type, @PathVariable("aaa") String name) {
        return "index/demo";
    }

}
