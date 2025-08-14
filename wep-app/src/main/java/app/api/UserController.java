package app.api;

import org.jmouse.mvc.Model;
import org.jmouse.mvc.NotFoundException;
import org.jmouse.web.annotation.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpMethod;

@Controller
public class UserController {

    @Mapping(path = "/welcome/{id}", httpMethod = HttpMethod.GET)
    public String welcome() {
        return "view:index/home";
    }

    @MethodDescription("Hello World!")
    @GetMapping(requestPath = "/customer/welcome/{id:int}")
    public String hello(WebBeanContext webBeanContext, Model model) {
        model.addAttribute("ID", 9801);
        return "view:index/demo";
    }

    @ExceptionHandler(NotFoundException.class)
    public String notFoundExceptionHandler(Model model, NotFoundException e) {
        model.addAttribute("message", e.getMessage());
        model.addAttribute("stackTrace", e.getStackTrace());
        return "view:index/error";
    }


}
