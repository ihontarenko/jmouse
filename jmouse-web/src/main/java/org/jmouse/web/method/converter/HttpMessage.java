package org.jmouse.web.method.converter;

import org.jmouse.web.http.request.Headers;

/**
 * 📦 Represents the common contract for HTTP input/output messages.
 *
 * <p>Provides access to HTTP {@link Headers} for both requests and responses.
 * Used as the base abstraction for {@link HttpInputMessage} and {@link HttpOutputMessage}.</p>
 *
 * @author Ivan
 */
public interface HttpMessage {

    /**
     * 📑 Returns the HTTP headers of this message.
     *
     * @return {@link Headers} instance (never {@code null})
     */
    Headers getHeaders();
}
