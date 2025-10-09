package org.jmouse.security.web.session;

import org.jmouse.web.http.RequestContextKeeper;

import java.io.IOException;

public class RedirectInvalidSessionHandler implements SessionInvalidHandler {

    private final String location;

    public RedirectInvalidSessionHandler(String location) {
        this.location = location;
    }

    @Override
    public void onInvalidSession(RequestContextKeeper keeper) throws IOException {
        keeper.response().sendRedirect(location);
    }

}
