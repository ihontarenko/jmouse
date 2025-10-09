package org.jmouse.security.web.authentication.ui;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.MediaType;
import org.jmouse.web.http.HeaderWriter;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.View;
import org.jmouse.web.servlet.filter.BeanFilter;
import org.jmouse.security.web.RequestMatcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.jmouse.security.web.authentication.identity.SubmitFormRequestAuthenticationFilter.JMOUSE_USER_IDENTITY_PASSWORD;
import static org.jmouse.security.web.authentication.identity.SubmitFormRequestAuthenticationFilter.JMOUSE_USER_IDENTITY_USERNAME;
import static org.jmouse.web.http.HttpStatus.OK;

public final class DefaultLoginPageGeneratingFilter implements BeanFilter, HeaderWriter {

    private final String         action;
    private final View           view;
    private final String         usernameParameter;
    private final String         passwordParameter;
    private final RequestMatcher requestMatcher;

    public DefaultLoginPageGeneratingFilter(
            View view, String action, String usernameParameter, String passwordParameter
    ) {
        this.requestMatcher = RequestMatcher.pathPattern(action);
        this.action = requireNonNull(action);
        this.view = view;
        this.usernameParameter = (usernameParameter != null ? usernameParameter : JMOUSE_USER_IDENTITY_USERNAME);
        this.passwordParameter = (passwordParameter != null ? passwordParameter : JMOUSE_USER_IDENTITY_PASSWORD);
    }

    @Override
    public void doFilterInternal(RequestContext requestContext, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  request = requestContext.request();
        HttpServletResponse response = requestContext.response();

        if (!requestMatcher.matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        boolean error  = request.getParameter("error")  != null;
        boolean logout = request.getParameter("logout") != null;

        writeHeaders(requestContext);

        renderView(error, logout, requestContext);
    }

    private void renderView(boolean error, boolean logout, RequestContext requestContext) {
        Map<String, Object> model = new HashMap<>();

        model.put("submitAction", action);
        model.put("usernameParameter", usernameParameter);
        model.put("passwordParameter", passwordParameter);
        model.put("isError", error);
        model.put("isLogout", logout);

        view.render(model, requestContext.request(), requestContext.response());
    }

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(OK.getCode());
        response.setContentType(MediaType.TEXT_HTML_VALUE);
    }

}
