package org.jmouse.web.security._old.http;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.security._old.CredentialCarrier;

import java.util.Map;

public class HttpCarrier implements CredentialCarrier {

    private final HttpServletRequest request;

    public HttpCarrier(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String first(String key) {
        return "";
    }

    @Override
    public Map<String, String> all() {
        return Map.of();
    }

}
