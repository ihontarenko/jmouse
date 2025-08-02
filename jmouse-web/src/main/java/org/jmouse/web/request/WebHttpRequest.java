package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jmouse.core.MediaType;
import org.jmouse.core.MimeParser;
import org.jmouse.util.Streamable;
import org.jmouse.web.request.http.HttpHeader;
import org.jmouse.web.request.http.HttpMethod;

import java.util.Collections;
import java.util.List;

public class WebHttpRequest extends HttpServletRequestWrapper implements WebRequest {

    private HttpMethod      httpMethod;
    private String          mappingPath;
    private List<MediaType> consumes = Collections.emptyList();
    private List<MediaType> produces = Collections.emptyList();

    public WebHttpRequest(HttpServletRequest request) {
        super(request);
        performRequest(request);
    }

    private void performRequest(HttpServletRequest request) {
        this.httpMethod = HttpMethod.valueOf(request.getMethod());

        String      mappingPath = request.getRequestURI();
        RequestPath requestPath = RequestAttributesHolder.getRequestPath();

        if (requestPath != null) {
            mappingPath = requestPath.requestPath();
        }

        this.mappingPath = mappingPath;

        this.consumes = Streamable.of(MimeParser.parseMimeTypes(
                request.getHeader(HttpHeader.ACCEPT.value()))).map(MediaType::new).toList();

        if (request.getContentType() != null) {
            this.produces = Streamable.of(MimeParser.parseMimeTypes(
                    request.getContentType())).map(MediaType::new).toList();
        }
    }

    @Override
    public HttpServletRequest getRequest() {
        return this;
    }

    @Override
    public String getMappingPath() {
        return mappingPath;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    @Override
    public List<MediaType> getProduces() {
        return List.copyOf(produces);
    }

    @Override
    public List<MediaType> getConsumes() {
        return List.copyOf(consumes);
    }
}
