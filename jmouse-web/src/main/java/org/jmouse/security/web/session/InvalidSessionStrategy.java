package org.jmouse.security.web.session;

import org.jmouse.web.http.request.RequestContextKeeper;

import java.io.IOException;

public interface InvalidSessionStrategy {
    void onInvalidSession(RequestContextKeeper keeper) throws IOException;
}
