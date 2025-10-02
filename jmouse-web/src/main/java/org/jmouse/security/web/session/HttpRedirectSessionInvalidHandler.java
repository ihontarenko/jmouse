package org.jmouse.security.web.session;

import org.jmouse.web.http.request.RequestContextKeeper;

import java.io.IOException;

public class HttpRedirectSessionInvalidHandler implements SessionInvalidHandler {

    private final String location;

    public HttpRedirectSessionInvalidHandler(String location) {
        this.location = location;
    }

    @Override
    public void onInvalidSession(RequestContextKeeper keeper) throws IOException {
        keeper.response().sendRedirect(location);
    }

}
