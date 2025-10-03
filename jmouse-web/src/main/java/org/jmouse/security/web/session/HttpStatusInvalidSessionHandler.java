package org.jmouse.security.web.session;

import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.RequestContextKeeper;

import java.io.IOException;

public class HttpStatusInvalidSessionHandler implements SessionInvalidHandler {

    private final HttpStatus status;

    public HttpStatusInvalidSessionHandler(HttpStatus status) {
        this.status = status;
    }

    @Override
    public void onInvalidSession(RequestContextKeeper keeper) throws IOException {
        keeper.response().setStatus(status.getCode());
    }

}
