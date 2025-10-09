package org.jmouse.web.http.multipart;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;
import org.jmouse.web.http.Headers;
import org.jmouse.web.http.RequestAttributesHolder;

/**
 * 📂 Strategy for resolving multipart requests.
 *
 * <p>Responsible for detecting multipart content and wrapping
 * {@link HttpServletRequest} into a multipart-capable request.</p>
 */
public interface MultipartResolver {

    /**
     * 🔄 Wrap the given request into a multipart-capable request.
     *
     * @param request original servlet request
     * @return wrapped request with multipart support
     */
    HttpServletRequest wrapRequest(HttpServletRequest request);

    /**
     * ✅ Check whether the given request is a multipart request.
     *
     * <p>Detection logic:</p>
     * <ul>
     *   <li>Uses {@link RequestAttributesHolder} headers if available</li>
     *   <li>Falls back to raw {@link HttpServletRequest#getContentType()}</li>
     *   <li>Checks compatibility with {@code multipart/form-data}</li>
     * </ul>
     *
     * @param request servlet request
     * @return {@code true} if request is multipart
     */
    default boolean isMultipart(HttpServletRequest request) {
        Headers headers = RequestAttributesHolder.getRequestHeaders().headers();
        MediaType contentType;

        if (headers != null && (contentType = headers.getContentType()) != null) {
            return contentType.compatible(MediaType.MULTIPART_FORM_DATA);
        } else {
            String rawContentType = request.getContentType();
            if (rawContentType != null) {
                return MediaType.forString(rawContentType).compatible(MediaType.MULTIPART_FORM_DATA);
            }
        }

        return false;
    }
}
