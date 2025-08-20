package org.jmouse.web.http.request.values;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.WebRequest;

import java.util.HashSet;
import java.util.Set;

public class HeaderValueProvider implements RequestValueProvider {

    private final Set<HttpHeader> headerNames;

    public HeaderValueProvider(HttpHeader... headerNames) {
        this(Set.of(headerNames));
    }

    public HeaderValueProvider(Set<HttpHeader> headerNames) {
        this.headerNames = headerNames;
    }

    @Override
    public String getTargetName() {
        return "HEADERS";
    }

    @Override
    public Set<String> getRequestValues(HttpServletRequest request) {
        Set<String> values = new HashSet<>();

        if (request instanceof WebRequest webRequest) {
            Headers headers = webRequest.getHeaders();
            for (HttpHeader headerName : headerNames) {
                if (headers.getHeader(headerName) instanceof String string) {
                    values.add(string);
                }
            }
        }

        return values;
    }

}
