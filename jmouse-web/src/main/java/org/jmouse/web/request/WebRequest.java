package org.jmouse.web.request;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.web.request.http.HttpMethod;

import java.util.List;

public interface WebRequest extends RequestAttributes {

    HttpServletRequest getRequest();

    String getMappingPath();

    HttpMethod getHttpMethod();

    List<MediaType> getProduces();

    List<MediaType> getConsumes();

}
