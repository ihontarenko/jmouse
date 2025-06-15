package org.jmouse.mvc;

import org.jmouse.web.request.http.HttpStatus;

public interface HandlerResponse {

    HttpStatus getHttpStatus();

    Object getReturnValue();

}
