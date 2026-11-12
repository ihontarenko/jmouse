package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;
import org.jmouse.http.Headers;
import org.jmouse.web.http.RequestAttributesHolder;

/**
 * Reads input metadata from the request's {@code Content-Type} header.
 * Deliberately not a response MediaTypeLookup: an input format never expresses response preferences.
 *
 * <p>Returns a single-element list when the header is present, otherwise an empty list.
 * Uses {@link RequestAttributesHolder} to access the current request headers.</p>
 *
 * <p>Priority is set relatively low so that more specific resolvers (e.g., route metadata)
 * can override it when applicable.</p>
 *
 * @see org.jmouse.http.HttpHeader#CONTENT_TYPE
 * @see MediaTypeLookup
 */
@Priority(Integer.MIN_VALUE + 3000)
public class ContentTypeHeaderLookup {

    /**
     * Looks up the {@link MediaType} from the {@code Content-Type} request header.
     *
     * @param request current HTTP request (not directly used; headers are read via {@link RequestAttributesHolder})
     * @return a singleton list with the resolved media type, or an empty list if absent
     */
    public List<MediaType> lookup(HttpServletRequest request) {
        Headers headers = RequestAttributesHolder.getRequestHeaders().headers();
        MediaType contentType = headers.getContentType();

        if (contentType != null) {
            return Collections.singletonList(contentType);
        }

        return Collections.emptyList();
    }
}
