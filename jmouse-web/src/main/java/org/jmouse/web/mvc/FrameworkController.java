package org.jmouse.web.mvc;

import org.jmouse.context.BeanConditionIfProperty;
import org.jmouse.web.annotation.Controller;
import org.jmouse.web.annotation.ExceptionHandler;

@Controller
@BeanConditionIfProperty(name = "jmouse.mvc.frameworkController.enabled", value = "true")
public class FrameworkController {

    @ExceptionHandler(HandlerMappingException.class)
    public String exceptionHandler(Model model, Exception e) {
        model.addAttribute("message", e.getMessage());
        model.addAttribute("stackTrace", e.getStackTrace());
        return "view:jmouse/error";
    }

}
