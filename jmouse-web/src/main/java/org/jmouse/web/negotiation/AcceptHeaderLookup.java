package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.core.MimeParser;
import org.jmouse.core.Streamable;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.RequestHeaders;

import java.util.Collections;
import java.util.List;

public class AcceptHeaderLookup implements MediaTypeLookup {

    @Override
    public List<MediaType> lookup(HttpServletRequest request) {
        RequestHeaders  headers    = RequestAttributesHolder.getRequestHeaders();
        List<MediaType> mediaTypes = Collections.emptyList();

        if (headers != null && headers.headers() != null) {
            return headers.headers().getAccept();
        }

        String accept = request.getHeader(HttpHeader.ACCEPT.toString());

        if (accept != null) {
            mediaTypes = Streamable.of(MimeParser.parseMimeTypes(accept))
                    .map(MediaType::new).toList();
        }

        return List.copyOf(mediaTypes);
    }

}
