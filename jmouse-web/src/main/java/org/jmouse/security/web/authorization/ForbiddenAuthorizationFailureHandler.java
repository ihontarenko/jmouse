package org.jmouse.security.web.authorization;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.web.AuthorizationFailureHandler;

import java.io.IOException;

import static org.jmouse.web.http.HttpHeader.X_SECURITY_REASON;
import static org.jmouse.web.http.HttpStatus.FORBIDDEN;

public class ForbiddenAuthorizationFailureHandler implements AuthorizationFailureHandler {

    @Override
    public void onFailure(HttpServletRequest request, HttpServletResponse response, Exception exception)
            throws IOException {
        response.setStatus(FORBIDDEN.getCode());
        response.setHeader(X_SECURITY_REASON.value(), exception.getMessage());
    }

}
