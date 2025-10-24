package org.jmouse.web_app.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.beans.annotation.ProxiedBean;
import org.jmouse.core.Bytes;
import org.jmouse.core.MediaType;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.throttle.RateLimitEnable;
import org.jmouse.security.access.annotation.Authorize;
import org.jmouse.security.access.annotation.PreAuthorize;
import org.jmouse.web.http.HttpMethod;
import org.jmouse.web.http.multipart.UploadLimitExceededException;
import org.jmouse.web.annotation.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.ContentDisposition;
import org.jmouse.web.http.multipart.MultipartFile;
import org.jmouse.web.http.multipart.MultipartWebRequest;
import org.jmouse.web.mvc.cors.CorsMapping;
import org.jmouse.web_app.service.StringService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@CorsMapping(allowedHeaders = {
        HttpHeader.CONTENT_TYPE,
        HttpHeader.AUTHORIZATION,
        HttpHeader.X_TEXT
})
@Controller
@ProxiedBean
@RateLimitEnable
public class SharedController {

    private StringService stringService;

    @BeanConstructor
    public SharedController(StringService stringService) {
        this.stringService = stringService;
    }

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
    @Authorize("authentication.authenticated && !a:isAnonymous()")
    public Bytes bytes(@PathVariable("bytes") String bytes) {
        return Bytes.parse(bytes);
    }

    @PreAuthorize(value = "33 % 3 == 0", index = 666)
    @GetMapping(
            requestPath = "/shared/random",
            produces = {
                    MediaType.TEXT_PLAIN_VALUE,
                    MediaType.APPLICATION_JSON_VALUE
            }
    )
    public String random() {
        return stringService.getRandom();
    }

    public static void main(String[] args) throws NoSuchMethodException {
        Method               method     = SharedController.class.getMethod("random");
        AnnotationRepository repository = AnnotationRepository.ofAnnotatedElement(method);

        repository.get(Authorize.class)
                .get().toResolvedAttributeMapWithMetas();

        System.out.println(repository);
    }

}
