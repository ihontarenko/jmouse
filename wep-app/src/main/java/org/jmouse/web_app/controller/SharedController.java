package org.jmouse.web_app.controller;

import org.jmouse.mvc.Model;
import org.jmouse.mvc.mapping.annotation.Controller;
import org.jmouse.mvc.mapping.annotation.ExceptionHandler;
import org.jmouse.mvc.mapping.annotation.GetMapping;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class SharedController {

    @GetMapping(requestPath = "/shared/illegalStateException")
    public String illegalStateException() {
        throw new IllegalStateException("test illegal state exception");
    }

    @GetMapping(requestPath = "/shared/unsupportedOperationException")
    public String unsupportedOperationException() {
        throw new UnsupportedOperationException("unsupported operation exception");
    }

    @GetMapping(requestPath = "/shared/ioException")
    public void ioException() throws IOException {
        throw new IOException("IO exception");
    }

    @GetMapping(requestPath = "/shared/runtimeException")
    public void runtimeException() throws IOException {
        throw new RuntimeException("RuntimeException");
    }

    @ExceptionHandler(IllegalStateException.class)
    public String illegalStateExceptionHandler(Model model, IllegalStateException e) {
        model.addAttribute("message", e.getMessage());
        return "view:index/error";
    }

    @ExceptionHandler(IOException.class) public String handleIO(IOException ex) {
        return "IOException";
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public Map<String, String> unsupportedOperationExceptionHandler(Exception e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseModel runtimeExceptionHandler(Exception e) {
        return new ResponseModel(Map.of("error", e.getMessage(), "exception", e));
    }

    @GetMapping(requestPath = "/shared/returnValueTest", produces = {
            "application/yaml",
//            "application/json"
    })
    public ResponseModel returnValueTest() throws IOException {
        return new ResponseModel(Map.of("text", "Hello World!", "IDs", List.of(1, 2, 3)));
    }

}
