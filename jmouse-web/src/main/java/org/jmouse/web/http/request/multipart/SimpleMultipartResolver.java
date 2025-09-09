package org.jmouse.web.http.request.multipart;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 📂 Simple {@link MultipartResolver} implementation.
 *
 * <p>Wraps an incoming {@link HttpServletRequest} into a
 * {@link MultipartWebHttpRequest} to provide multipart parsing support.</p>
 */
public class SimpleMultipartResolver implements MultipartResolver {

    /**
     * 🔄 Wrap the given request into a multipart-capable request.
     *
     * @param request original servlet request
     * @return wrapped {@link MultipartWebHttpRequest}
     */
    @Override
    public HttpServletRequest wrapRequest(HttpServletRequest request) {
        return new MultipartWebHttpRequest(request);
    }
}
