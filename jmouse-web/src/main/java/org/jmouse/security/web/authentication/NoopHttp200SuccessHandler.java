package org.jmouse.security.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.web.http.HttpStatus;

public class NoopHttp200SuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onSuccess(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpStatus.OK.getCode());
    }

}
