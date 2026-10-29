package org.jmouse.web.http.cache;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** No saved request or session allocation for stateless authentication. */
public final class NullRequestCache implements RequestCache {
    @Override
    public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
    }

    @Override
    public SavedRequest getRequest(HttpServletRequest request, HttpServletResponse response) {
        return null;
    }

    @Override
    public void removeRequest(HttpServletRequest request, HttpServletResponse response) {
    }
}
