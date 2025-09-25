package org.jmouse.web.security._old.http;

import org.jmouse.security._old.Decision;
import org.jmouse.security._old.translate.DecisionApplier;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.HttpStatus;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.WWWAuthenticate;

public final class HttpDecisionApplier implements DecisionApplier {

    private final Headers headers;

    public HttpDecisionApplier(Headers headers) {
        this.headers = headers;
    }

    @Override
    public void apply(Decision decision) {
        switch (decision.effect()) {
            case PERMIT -> {

            }
            case DENY -> {
                headers.setStatus(HttpStatus.FORBIDDEN);
                headers.setHeader(HttpHeader.X_SECURITY_REASON, decision.message());
            }
            case CHALLENGE -> {
                WWWAuthenticate wwwAuthenticate = WWWAuthenticate.basic("jMouse");
                headers.setStatus(HttpStatus.UNAUTHORIZED);
                headers.setHeader(wwwAuthenticate.toHttpHeader(), wwwAuthenticate.toHeaderValue());
            }
            case REDIRECT -> {
                String location = String.valueOf(decision.attributes().get("location"));
                if (location == null || location.isBlank()) {
                    headers.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                    headers.setHeader(HttpHeader.X_SECURITY_REASON, "NO LOCATION SPECIFIED!");
                } else {
                    headers.setStatus(HttpStatus.FOUND);
                    headers.setHeader(HttpHeader.LOCATION, location);
                }
            }
            case DEFER -> {
                // NO-OP!
            }
        }
    }

}
