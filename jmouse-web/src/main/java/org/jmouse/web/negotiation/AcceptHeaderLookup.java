package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.core.MimeParser;
import org.jmouse.core.Priority;
import org.jmouse.core.Streamable;
import org.jmouse.web.http.HttpHeader;
import org.jmouse.web.http.request.RequestAttributesHolder;
import org.jmouse.web.http.request.RequestHeaders;

import java.util.Collections;
import java.util.List;

/**
 * 📡 Resolves {@link MediaType} values from the
 * HTTP {@code Accept} header.
 *
 * <p>Registered with the highest priority, so it is
 * evaluated before other strategies.</p>
 */
@Priority(Integer.MIN_VALUE)
public class AcceptHeaderLookup implements MediaTypeLookup {

    /**
     * 🔎 Lookup media types declared in the {@code Accept} header.
     *
     * <ul>
     *   <li>Uses {@link RequestHeaders} from {@link RequestAttributesHolder} if available.</li>
     *   <li>Otherwise falls back to parsing the raw HTTP header.</li>
     * </ul>
     *
     * @param request current HTTP request
     * @return immutable list of resolved media types, or empty if none
     */
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
                    .map(MediaType::new)
                    .toList();
        }

        return List.copyOf(mediaTypes);
    }

}
