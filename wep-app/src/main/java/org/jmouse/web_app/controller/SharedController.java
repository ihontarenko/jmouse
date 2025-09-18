package org.jmouse.web_app.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.annotation.ProxiedBean;
import org.jmouse.core.Bytes;
import org.jmouse.core.throttle.RateLimit;
import org.jmouse.core.throttle.RateLimitEnable;
import org.jmouse.core.throttle.RateLimited;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.request.multipart.UploadLimitExceededException;
import org.jmouse.web.annotation.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.request.ContentDisposition;
import org.jmouse.web.http.request.multipart.MultipartFile;
import org.jmouse.web.http.request.multipart.MultipartWebRequest;
import org.jmouse.web.mvc.cors.CorsMapping;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.jmouse.core.throttle.RateLimit.Scope.METHOD;

@CorsMapping(allowedHeaders = {
        HttpHeader.CONTENT_TYPE,
        HttpHeader.AUTHORIZATION,
        HttpHeader.X_TEXT
})
@Controller
@ProxiedBean
@RateLimitEnable
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

    @ExceptionHandler(IOException.class) public String handleIO(IOException ex) {
        return "IOException";
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public Map<String, String> unsupportedOperationExceptionHandler(Exception e) {
        return Map.of("unsupportedOperationException", e.getMessage());
    }

    @ExceptionHandler(UploadLimitExceededException.class)
    public Map<String, String> uploadLimitExceededExceptionHandler(Exception e) {
        return Map.of("error", e.getMessage(), "cause", e.getCause().getMessage());
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

    @GetMapping(requestPath = "/shared/streamData", produces = {
            "application/octet-stream"
    })
    public byte[] streamData() {
        return new ResponseModel(Map.of("text", "Hello World!", "IDs", List.of(1, 2, 3))).toString().getBytes();
    }

    @PostMapping(requestPath = "/shared/postRequest", produces = "image/jpeg")
    public byte[] postRequest(HttpServletRequest request, HttpServletResponse response, WebBeanContext context){

        if (request instanceof MultipartWebRequest multipartWebRequest) {
            MultipartFile file = multipartWebRequest.getFirstFile("file");

            try {
                response.setHeader(HttpHeader.CONTENT_DISPOSITION.value(), ContentDisposition.create()
                        .type("inline").filename("uploaded.jpg").build().toString());
                return file.getBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new ResponseModel(Map.of("text", "Hello World!", "IDs", List.of(1, 2, 3))).toString().getBytes();
    }

    @GetMapping(requestPath = "/shared/mapData")
    public Map<String, Object> mapData() {
        return Map.of("text", "Hello World!", "IDs", List.of(1, 2, 3));
    }

    @GetMapping(requestPath = "/shared/mapData.{format:json|xml|yaml}")
    public Map<String, Object> mapDataPath(@PathVariable("format") String format) {
        return Map.of("text", "Hello World!", "IDs", List.of(1, 2, 3), "format", format);
    }

    @CorsMapping(
            origins = {"https://*.youtube.com"},
            methods = {HttpMethod.GET, HttpMethod.POST},
            exposedHeaders = {HttpHeader.CONTENT_LENGTH},
            allowCredentials = true,
            maxAge = 3600
    )
    @GetMapping(
            requestPath = "/shared/{format}/bytes/{bytes}"
    )
    public Bytes bytesA(@PathVariable("bytes") String bytes) {
        return Bytes.parse(bytes);
    }

    @CorsMapping(
            origins = {"megogo.net"},
            methods = {HttpMethod.GET, HttpMethod.POST},
            exposedHeaders = {HttpHeader.CONTENT_LENGTH},
            allowCredentials = true,
            maxAge = 3600
    )
    @GetMapping(
            requestPath = "/shared/{format}/bytes/{bytes}/megogo"
    )
    @RateLimit(name = "megogo call", max = 1, per = ChronoUnit.SECONDS, amount = 2, scope = METHOD)
    public Bytes bytesB(@PathVariable("bytes") String bytes) {
        return Bytes.parse(bytes);
    }

    @GetMapping(requestPath = "/shared/userModel/{name}")
    public UserModel userModel(@PathVariable("name") String name) {
        return new UserModel(name, "passwd!");
    }

}
