package app.api;

import org.jmouse.mvc.Model;
import org.jmouse.mvc.NotFoundException;
import org.jmouse.web.annotation.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.request.http.HttpMethod;

@Controller
public class UserController {

    @Mapping(path = "/customer/{id}", httpMethod = HttpMethod.GET)
    public String index() {
        return "view:index/home";
    }

    @GetMapping(requestPath = "/customer/demo/{id}")
    public String demo() {
        return "view:index/demo";
    }

    @MethodDescription("Hello World!")
    @GetMapping(requestPath = "/customer/welcome/{id:int}")
    public String hello(WebBeanContext webBeanContext) {
        return "view:index/demo";
    }

    @ExceptionHandler(NotFoundException.class)
    public String notFoundExceptionHandler(Model model, NotFoundException e) {
        model.addAttribute("message", e.getMessage());
        model.addAttribute("stackTrace", e.getStackTrace());
        return "view:index/error";
    }


}
