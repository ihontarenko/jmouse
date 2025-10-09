package org.jmouse.security.web.authentication.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.security.web.authentication.AuthenticationSuccessHandler;
import org.jmouse.web.http.cache.RequestCache;
import org.jmouse.web.http.cache.SavedRequest;

import java.io.IOException;

public class SavedRequestAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache;
    private       String       defaultLocation = "/";

    public SavedRequestAwareAuthenticationSuccessHandler(RequestCache cache) {
        this.requestCache = cache;
    }

    public SavedRequestAwareAuthenticationSuccessHandler defaultTargetUrl(String location) {
        this.defaultLocation = location;
        return this;
    }

    @Override
    public void onSuccess(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SavedRequest saved    = requestCache.getRequest(request, response);
        String       location = defaultLocation;

        if (saved != null) {
            location = saved.getRedirectUrl();
            requestCache.removeRequest(request, response);
        }

        response.sendRedirect(location);
    }

}
