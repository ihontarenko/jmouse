package org.jmouse.web.security.http;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security.*;

public final class HttpEnvelopeFactory {

    public static Envelope ofHttpRequest(HttpServletRequest request, Resource resource, Operation operation) {
        return new SecurityEnvelope(
                Subjects.anonymous(), resource, operation, Attributes.mutable(), new HttpCarrier(request));
    }

}
