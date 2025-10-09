package org.jmouse.web.security.firewall;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.RequestContext;

public record EvaluationInput(
        RequestContext requestContext
) {
    public static EvaluationInput from(HttpServletRequest request) {
        return new EvaluationInput(new RequestContext(request, null));
    }
}