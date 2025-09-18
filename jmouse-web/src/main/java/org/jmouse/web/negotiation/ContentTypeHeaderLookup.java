package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.core.Priority;
import org.jmouse.web.http.request.Headers;
import org.jmouse.web.http.request.RequestAttributesHolder;

import java.util.Collections;
import java.util.List;

/**
 * Resolves the media type from the request's {@code Content-Type} header.
 *
 * <p>Returns a single-element list when the header is present, otherwise an empty list.
 * Uses {@link RequestAttributesHolder} to access the current request headers.</p>
 *
 * <p>Priority is set relatively low so that more specific resolvers (e.g., route metadata)
 * can override it when applicable.</p>
 *
 * @see org.jmouse.web.http.HttpHeader#CONTENT_TYPE
 * @see MediaTypeLookup
 */
@Priority(Integer.MIN_VALUE + 3000)
public class ContentTypeHeaderLookup implements MediaTypeLookup {

    /**
     * Looks up the {@link MediaType} from the {@code Content-Type} request header.
     *
     * @param request current HTTP request (not directly used; headers are read via {@link RequestAttributesHolder})
     * @return a singleton list with the resolved media type, or an empty list if absent
     */
    @Override
    public List<MediaType> lookup(HttpServletRequest request) {
        Headers   headers     = RequestAttributesHolder.getRequestHeaders().headers();
        MediaType contentType = headers.getContentType();

        if (contentType != null) {
            return Collections.singletonList(contentType);
        }

        return Collections.emptyList();
    }
}
