package org.jmouse.web.match.routing.condition;

import org.jmouse.core.matcher.Match;
import org.jmouse.web.match.routing.MappingMatcher;
import org.jmouse.web.http.RequestRoute;
import org.jmouse.web.http.HttpHeader;

/**
 * 📡 Matches a specific HTTP header and its expected value.
 */
public final class HttpHeaderMatcher implements MappingMatcher<RequestRoute> {

    private final HttpHeader header;
    private final Object requiredValue;

    public HttpHeaderMatcher(HttpHeader header, Object requiredValue) {
        this.header = header;
        this.requiredValue = requiredValue;
    }

    @Override
    public Match apply(RequestRoute route) {
        Object value = route.headers().getHeader(header);

        if (value != null && value.equals(requiredValue)) {
            return Match.hit()
                    .attach(HttpHeader.class, header)
                    .attach(Facet.class, new Facet(header, requiredValue));
        }

        return Match.miss();
    }

    @Override
    public int compare(MappingMatcher<?> other, RequestRoute ctx) {
        if (other instanceof HttpHeaderMatcher o) {
            return header.compareTo(o.header);
        }
        return 0;
    }

    @Override
    public String toString() {
        return "HttpHeaderMatcher[" + header + "=" + requiredValue + "]";
    }

    public record Facet(HttpHeader httpHeader, Object value) {
    }

}
