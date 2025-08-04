package app.api;

import org.jmouse.mvc.mapping.annnotation.Controller;
import org.jmouse.mvc.mapping.annnotation.GetMapping;
import org.jmouse.mvc.mapping.annnotation.Mapping;
import org.jmouse.mvc.mapping.annnotation.MethodDescription;
import org.jmouse.web.request.http.HttpMethod;

@Controller
public class UserController {

    @Mapping(path = "/customer/{id}", httpMethod = HttpMethod.GET)
    public String index() {
        return "index/home";
    }

    @GetMapping(requestPath = "/customer/demo/{id}")
    public String demo() {
        return "index/demo";
    }

    @MethodDescription("Hello World!")
    @GetMapping(requestPath = "/customer/welcome/{id:int}")
    public String hello() {
        return "index/demo";
    }

}
