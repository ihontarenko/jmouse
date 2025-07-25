package org.jmouse.mvc;

import org.jmouse.web.request.http.HttpStatus;

public interface HandlerResult {

    HttpStatus getHttpStatus();

    Object getReturnValue();

}
