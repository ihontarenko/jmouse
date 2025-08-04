package org.jmouse.mvc;

import org.jmouse.web.request.Headers;
import org.jmouse.web.request.http.HttpStatus;

public interface ModelContext {

    Model model();

    Headers headers();

    HttpStatus httpStatus();

    void httpStatus(HttpStatus status);

    record Default(Model model, Headers headers, HttpStatus httpStatus) implements ModelContext {

        @Override
        public void httpStatus(HttpStatus status) {

        }

    }

}
