package org.jmouse.web_app.controller;

import org.jmouse.mvc.mapping.annnotation.Controller;
import org.jmouse.mvc.mapping.annnotation.ExceptionHandler;
import org.jmouse.mvc.mapping.annnotation.GetMapping;

import java.util.Map;

@Controller
public class SharedController {

    @GetMapping(requestPath = "/shared/illegalStateException")
    public String illegalStateException() {
        throw new IllegalStateException();
    }

    @GetMapping(requestPath = "/shared/unsupportedOperationException")
    public String unsupportedOperationException() {
        throw new UnsupportedOperationException();
    }

    @ExceptionHandler(IllegalStateException.class)
    public String illegalStateExceptionHandler() {
        return "view:index/error";
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public Map<String, String> unsupportedOperationExceptionHandler() {
        return Map.of("error", "unsupported");
    }

}
