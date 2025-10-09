package org.jmouse.web.mvc;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.ETag;
import org.jmouse.web.mvc.method.HandlerMethod;

public interface ETagProvider {

    ETag provide(HandlerMethod handlerMethod, MappingResult mappingResult, HttpServletRequest request);

}
