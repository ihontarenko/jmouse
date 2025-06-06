package org.jmouse.mvc;

import org.jmouse.web.http.HttpStatus;

public interface HandlerResponse {

    HttpStatus getHttpStatus();

    Object getReturnValue();

}
